
# Script Oracle : Partitionnement des Données

Ce script explore les différentes méthodes de partitionnement dans Oracle, notamment :
- **Partitionnement par lignes (LIST)** : Organisation des données en fonction des valeurs spécifiques d'une colonne.
- **Partitionnement par colonnes** : Division logique des données en tables distinctes.
- **Partitionnement hybride** : Combinaison des deux approches précédentes.

Chaque section du script est accompagnée d'exemples pour tester et comprendre le partitionnement.

---

## 1. Partitionnement par Lignes (LIST)

Le partitionnement par **LIST** consiste à séparer les données en partitions basées sur des valeurs spécifiques.

### Création de la Table Clients
```sql
CREATE TABLE Clients (
    Client_ID NUMBER PRIMARY KEY,
    Nom VARCHAR2(100),
    Adresse VARCHAR2(255),
    Pays VARCHAR2(50),
    Date_Inscription DATE
)
PARTITION BY LIST (Pays) (
    PARTITION partition_france VALUES ('France'),
    PARTITION partition_usa VALUES ('USA'),
    PARTITION partition_canada VALUES ('Canada'),
    PARTITION partition_autres VALUES (DEFAULT)
);
```

### Insertion des Données
```sql
INSERT INTO Clients VALUES (1, 'Alice Dupont', '10 rue des Lilas, Paris', 'France', TO_DATE('2022-01-10', 'YYYY-MM-DD'));
INSERT INTO Clients VALUES (2, 'John Smith', '123 Main St, New York', 'USA', TO_DATE('2021-05-15', 'YYYY-MM-DD'));
-- Ajoutez les autres lignes ici...
COMMIT;
```

### Requêtes de Test
- Requête ciblant une partition spécifique (`partition_france`) :
  ```sql
  SELECT * 
  FROM Clients PARTITION (partition_france);
  ```
- Requête sur plusieurs partitions :
  ```sql
  SELECT * 
  FROM Clients
  WHERE Pays IN ('USA', 'Canada');
  ```

---

## 2. Partitionnement par Colonnes

Le partitionnement par colonnes divise les données en plusieurs tables logiques selon leur nature (statique ou dynamique).

### Création des Tables
#### Table des Attributs Statiques
```sql
CREATE TABLE Clients_Statique (
    Client_ID NUMBER PRIMARY KEY,
    Nom VARCHAR2(100),
    Date_Inscription DATE
);
```

#### Table des Attributs Dynamiques
```sql
CREATE TABLE Clients_Dynamique (
    Client_ID NUMBER PRIMARY KEY,
    Adresse VARCHAR2(255),
    Indice_Confiance NUMBER
);
```

### Insertion des Données
```sql
-- Insertion dans Clients_Statique
INSERT INTO Clients_Statique VALUES (1, 'Alice Dupont', TO_DATE('2022-01-10', 'YYYY-MM-DD'));
-- Ajoutez les autres lignes ici...

-- Insertion dans Clients_Dynamique
INSERT INTO Clients_Dynamique VALUES (1, '10 rue des Lilas, Paris', 85);
-- Ajoutez les autres lignes ici...

COMMIT;
```

### Requêtes de Test
- Requête sur les données statiques uniquement :
  ```sql
  SELECT * 
  FROM Clients_Statique
  WHERE Date_Inscription > TO_DATE('2022-01-01', 'YYYY-MM-DD');
  ```
- Requête sur les données dynamiques uniquement :
  ```sql
  SELECT * 
  FROM Clients_Dynamique
  WHERE Indice_Confiance > 80;
  ```

---

## 3. Partitionnement Hybride (LIST + Colonnes)

### Création des Tables Partitionnées
#### Clients_Statique Partitionné
```sql
CREATE TABLE Clients_Statique (
    Client_ID NUMBER PRIMARY KEY,
    Nom VARCHAR2(100),
    Date_Inscription DATE,
    Pays VARCHAR2(50)
)
PARTITION BY LIST (Pays) (
    PARTITION partition_france VALUES ('France'),
    PARTITION partition_usa VALUES ('USA'),
    PARTITION partition_canada VALUES ('Canada'),
    PARTITION partition_autres VALUES (DEFAULT)
);
```

#### Clients_Dynamique Partitionné
```sql
CREATE TABLE Clients_Dynamique (
    Client_ID NUMBER PRIMARY KEY,
    Adresse VARCHAR2(255),
    Indice_Confiance NUMBER,
    Pays VARCHAR2(50)
)
PARTITION BY LIST (Pays) (
    PARTITION partition_france VALUES ('France'),
    PARTITION partition_usa VALUES ('USA'),
    PARTITION partition_canada VALUES ('Canada'),
    PARTITION partition_autres VALUES (DEFAULT)
);
```

### Requêtes de Test
1. **Cibler une partition spécifique :**
   ```sql
   SELECT * 
   FROM Clients_Statique PARTITION (partition_france)
   WHERE Date_Inscription > TO_DATE('2021-01-01', 'YYYY-MM-DD');
   ```

2. **Requête combinée via une vue :**
   ```sql
   CREATE OR REPLACE VIEW Clients_Complet AS
   SELECT 
       S.Client_ID,
       S.Nom,
       S.Date_Inscription,
       D.Adresse,
       D.Indice_Confiance,
       S.Pays
   FROM 
       Clients_Statique S
   JOIN 
       Clients_Dynamique D ON S.Client_ID = D.Client_ID AND S.Pays = D.Pays;

   SELECT * 
   FROM Clients_Complet
   WHERE Indice_Confiance > 80 AND Date_Inscription > TO_DATE('2022-01-01', 'YYYY-MM-DD');
   ```

---

## Conclusion

Ce script vous permet de découvrir et d'explorer :
- Le partitionnement par listes pour organiser les données en fonction des valeurs spécifiques.
- Le partitionnement par colonnes pour diviser logiquement les données.
- La combinaison de ces deux approches pour optimiser les requêtes.

Utilisez les exemples de requêtes pour analyser les performances et comprendre comment Oracle gère ces partitions.
