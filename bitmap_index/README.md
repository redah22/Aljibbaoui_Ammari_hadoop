Index BitMap
===

Les scripts mis à disposition permettent de familiariser avec la déclaration d'indexes de type bitmap et join-bitmap, ainsi qu'avec leur utilisation par l'optimiseur Oracle lorsqu'il essaye de construire le plan d'accès pour repondre à la requête.

Travail à faire : 
1. Exécuter les différentes parties du script et comprendre ce qui se passe dans chaque partie.
2. Donner d'autres exemples requêtes et qui utilisent et qui n'utilisent pas l'index


## Bitmap (no join)

Petit nettoyage de la base (au cas où vous aviez déjà exécuté ce script)
```sql
BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_rating';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_gender';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

DROP TABLE IF EXISTS Customers CASCADE;
```

Commençons par créer une table dimensionelle pour les clients et y ajoutons des données.

```sql
CREATE TABLE Customers (
    custid NUMBER PRIMARY KEY,
    name VARCHAR2(50),
    gender CHAR(1),
    rating NUMBER
);

INSERT INTO Customers (custid, name, gender, rating) VALUES (112, 'Joe', 'M', 3);
INSERT INTO Customers (custid, name, gender, rating) VALUES (115, 'Ram', 'M', 5);
INSERT INTO Customers (custid, name, gender, rating) VALUES (119, 'Sue', 'F', 5);
INSERT INTO Customers (custid, name, gender, rating) VALUES (116, 'Woo', 'M', 4);
INSERT INTO Customers (custid, name, gender, rating) VALUES (120, 'Ana', 'F', 3);
INSERT INTO Customers (custid, name, gender, rating) VALUES (121, 'Tom', 'M', 1);
INSERT INTO Customers (custid, name, gender, rating) VALUES (122, 'Eve', 'F', 2);

COMMIT;
```
### Voici le contenu de la table.

| custid | name | gender | rating |
|--------|------|--------|--------|
| 112    | Joe  | M      | 3      |
| 115    | Ram  | M      | 5      |
| 119    | Sue  | F      | 5      |
| 116    | Woo  | M      | 4      |
| 120    | Ana  | F      | 3      |
| 121    | Tom  | M      | 1      |
| 122    | Eve  | F      | 2      |

Continuons avec la création des index.

```sql
CREATE BITMAP INDEX idx_gender ON Customers (gender);
```

Voici le contenu de l'index 

| custid | Gender = M | Gender = F |
|--------|------------|------------|
| 112    | 1          | 0          |
| 115    | 1          | 0          |
| 119    | 0          | 1          |
| 116    | 1          | 0          |
| 120    | 0          | 1          |
| 121    | 1          | 0          |
| 122    | 0          | 1          |



Combien d'index sont utilisés par la requête suivante ?
```
-- sql
SELECT custid, name, gender, rating
FROM Customers
WHERE rating = 5 AND gender = 'M';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

Le script `bitmap_index.sql' présente également la création d'un index bitmap sur la colonne `rating`. 


## Join Bitmap

Avant de commencer, nous supprimons les tables existantes pour éviter les conflits ou les erreurs lors de la création des nouvelles tables.

```sql
-- Cleanup
BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_bj_product_id';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Fact_table_sales CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Dim_table_product CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/
```


Ensuite, nous allons créer des tables et y mettre quelques données. L'étape d'ajout des données est nécessaire pour que les indexes soient utilisés. En effet, pour des trop petits volumes de données leur utilisation pourrait être ignorée. 

```sql

-- Step 1: 
-- Create the Dimension Table 
CREATE TABLE Dim_table_product (
    product_id VARCHAR2(10),
    name VARCHAR2(50),
    price NUMBER
);

-- Create the Fact Table 
CREATE TABLE Fact_table_sales (
    sale_id VARCHAR2(10),
    product_id VARCHAR2(10),
    store_id VARCHAR2(10),
    sale_date DATE,
    amount NUMBER
);

Step 2: Populate the tables
-- Populate Dimension Table with 1000 Rows
BEGIN
    FOR i IN 1..1000 LOOP
        INSERT INTO Dim_table_product (product_id, name, price)
        VALUES ('p' || TO_CHAR(i), 'product_' || TO_CHAR(i), MOD(i, 100) + 10);
    END LOOP;
    COMMIT;
END;
/

-- Populate Fact Table with 10,000 Rows and Restrict Products to Even Numbers
BEGIN
    FOR i IN 1..10000 LOOP
        INSERT INTO Fact_table_sales (sale_id, product_id, store_id, sale_date, amount)
        VALUES (
            's' || TO_CHAR(i), 
            'p' || TO_CHAR(2 * MOD(i, 500) + 2), -- Only even product IDs, for data variability
            'store_' || TO_CHAR(MOD(i, 50) + 1), 
            SYSDATE, 
            MOD(i, 500) + 50
        );
    END LOOP;
    COMMIT;
END;
/
```

À ce point, lancez une requête pour visualer le contenu des deux tables!


Nous allons maintenant ajouter des index. Noter que le join-index doit nécessairement porter sur une clé primaire (dans ce cas l'identifiant du produit).

```sql
-- Step 3: Add Primary Keys
-- Primary Key for Dim_table_product
ALTER TABLE Dim_table_product ADD CONSTRAINT dim_product_pk PRIMARY KEY (product_id);

-- Step 4: Create the Bitmap Join Index on `product_id`
CREATE BITMAP INDEX idx_bj_product_id
ON Fact_table_sales(d.product_id)
FROM Fact_table_sales f, Dim_table_product d
WHERE f.product_id = d.product_id;
```

C'est le temps d'interroger la base. Quel est le plan d'exécution de cette requête ? Que fait l'optimiseur Oracle ? Est ce que la table `Dim_table_product` est utilisée lors de l'évaluation ?
```sql
EXPLAIN PLAN FOR
SELECT /*+ INDEX(f idx_bj_product_id) */
       f.sale_id
FROM Fact_table_sales f
JOIN Dim_table_product d
  ON f.product_id = d.product_id
WHERE d.product_id = 'p500';

-- Display Execution Plan
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

Deuxième requête. Que se passe-t-il maintenant ?
```sql

-- Step 6: Run another query ; force index usage
EXPLAIN PLAN FOR
SELECT /*+ INDEX(f idx_bj_product_id) */
       f.sale_id
FROM Fact_table_sales f
JOIN Dim_table_product d
  ON f.product_id = d.product_id
WHERE d.price = 50;

-- Display Execution Plan Again
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

Et si par hasard on ne forçait pas l'utilisation de l'inde ? Bah... avec des petits volumes de données l'optimiseur pourrait faire un autre choix.
```sql
EXPLAIN PLAN FOR
SELECT 
       f.sale_id
FROM Fact_table_sales f
JOIN Dim_table_product d
  ON f.product_id = d.product_id
WHERE d.price = 50;

-- Display Execution Plan Again
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

**Maintenant c'est à vous !**  Donner d'autres exemples requêtes et qui utilisent et qui n'utilisent pas l'index.