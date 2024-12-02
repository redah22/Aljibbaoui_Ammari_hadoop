## Il est important de lire tout le document.

## Ressources

- [MR1] MapReduce: Simplified Data Processing on Large Clusters - Jeffrey Dean and Sanjay Ghemawat
- [MR2] Apache Hadoop http://hadoop.apache.org/
- [MR3] Hadoop: the definitive guide (http://grut-computing.com/HadoopBook.pdf)

## Avant commencer

La programmation en Map-Reduce utilise le langage Java - avec ses avantages et inconvénients. Il est ainsi indispensable d'effectuer ce travail en binômes, de rester bien concentrés pour bien comprendre la cause des bogues (souvent ce seront des problèmes de typage ou nommage des ressources).  

## Format du rendu

Il est demandé de rédiger un document expliquant en quelques lignes comment vous avez répondu à chaque question du TP. Il est important de mettre en évidence juste les points les plus importants vous ayant permis de répondre à la question. Le code Java produit est également à rendre, mais dans une archive `.zip`.

## Installation

Let TP a été longuement testé avec Eclipse et Linux/OSX, et est conçu pour tourner sur les ordinateurs de la faculté.

Pour faire face aux différents soucis rencontres dans les différents systèmes d'exploitation (eg Windows) et IDE (IntelliJ, Eclipse) deux versions du TP sont mises à disposition.

Essayez d'importer et tester le fonctionnement du projet avec la `v1.2`. Si tout va bien, passez à la section suivante.

Problèmes avec IntelliJ : IntelliJ pourrait utiliser une JVM différente de celle indiquée dans le pom. Dans ce cas, selectionner Java 11 dans le menu File > Project Structure > Project > Project SDK.

Problèmes avec Windows : manque de fonctionnalités pour écriture sur disque via Hadoop. Essayez le projet avec la `v3.6`.

D'autres possibilités d'installation sont indiquées [ici](doc/install.md).

## FAQ

**Le log dans la sortie standard est illisible. Je ne comprends pas ce que fait mon programme Map-Reduce !**  
Réponse : Le programme lit les fichiers contenus dans le répertoire `input-wordCount/`, puis effectue un comptage des mots qui est sauvegardé dans le repertoire `output` (vérifiez-le !).

### Statistiques importantes lors de l'exécution d'un job Hadoop :

- **Map input records** : Le nombre de couples clé-valeur traitées avec appels à la fonction `map`.
- **Map output records** : Le nombre de couples clé-valeur produites par des appels à la fonction `map`.
- **Reduce input records** : Le nombre de couples clé-valeur traitées avec appels à la fonction `reduce`. Typiquement égal à `Map output records`, mais plus petit si certaines optimisations sont activées.
- **Reduce input groups** : Le nombre de clés distinctes traitées avec appels à la fonction `reduce`.
- **Reduce output records** : Le nombre de couples clé-valeur produites par des appels à la fonction `reduce`.

### Exemple HadoopPoem

- **Input** : Texte sur 17 lignes composé de 72 mots dont 59 distincts.
- **Map input records** : 17 (une par ligne).
- **Map output records** : 72 (une par mot).
- **Reduce input records** : 72.
- **Reduce input groups** : 59 (mots distincts).
- **Reduce output records** : 59 (calcul du nombre d’occurrences de chaque mot).

**Où sont les résultats de mon programme Map-Reduce ?**  
Voir le répertoire `output/wordCount-xxxx`.

**Y a-t-il de la parallélisation à l'état actuel ?**  
Non, on utilise Hadoop en mode Standalone pour ce TP (les plus courageux peuvent essayer la version "pseudo-distributed").

**Qu'est ce qu'on fait maintenant ?**  
Modifiez les programmes fournis pour implémenter les traitements ci-dessous. Comprenez d'abord le code.

**Important : Comment puis-je déboguer mon programme ?**  
Vous disposez d'un logger via la variable de classe `LOG`. Ces messages sont aussi enregistrés dans `TP_HMIN122M-hadoop/out.log`. Utilisez également le debugger d’Eclipse (Run/Debug ou `F11`).

---

## Exercices

### Exercice 0 - WordCount
Tester le programme WordCount.

### Exercice 1 - WordCount + Filter
Modifier la fonction reduce du programme WordCount.java pour afficher uniquement les mots ayant un nombre d’occurrences supérieur ou égal à deux.

### Exercice 2 - Group-By
Implémenter un opérateur de regroupement sur l'attribut `Customer-ID` dans GroupBy.java.  
Les données sont dans `input-groupBy` et doivent calculer le total des profits (`Profit`) par client.

### Exercice 3 - Group-By
Modifier le programme précédent :
1. Calculer les ventes par `Date` et `State`.
2. Calculer les ventes par `Date` et `Category`.
3. Calculer par commande :
   - Le nombre de produits distincts achetés.
   - Le nombre total d'exemplaires.

### Exercice 4 - Join
Créer une classe Join.java pour joindre les informations des clients et commandes dans `input-join`.  
Restituer les couples `(CUSTOMERS.name, ORDERS.comment)`.

**Note :** Copier les valeurs de l'itérateur dans un tableau temporaire et utiliser deux boucles imbriquées pour effectuer la jointure.
