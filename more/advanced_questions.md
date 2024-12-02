
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

### Exercice 5 - GroupBy + Join

Pour le fichier `superstore.csv`, calculer le montant total des achats faits par chaque client.  
**Le programme doit restituer des couples** `(CUSTOMERS.name, SUM(totalprice))`.

---

### Exercice 6 - Suppression des doublons (DISTINCT)

Donner la liste des clients (sans doublons) présents dans le dataset du répertoire `input-groupBy`.

---

### Exercice 7 - MR <-> SQL

Donner le code SQL équivalent aux traitements Map/Reduce implémentés pour les questions 4, 5, 6 et 7.

---

### Exercice 8 - TAM

Rendez-vous à l'adresse : [offre-de-transport-tam-en-temps-reel](http://data.montpellier3m.fr/dataset/offre-de-transport-tam-en-temps-reel)  
Télécharger le fichier `TAM_MMM_OffreJour.zip` contenant la prévision du service de tramway pour la journée.

Répondre aux questions suivantes :
- Donner un aperçu des trams et bus de la station `OCCITANIE`. Préciser le nombre de (bus ou trams) pour chaque heure et ligne.  
  Exemple : `<Ligne 1, 17h, 30>` (à 17h, 30 trams de la ligne 1).
- Pour chaque station, donner le nombre de trams et bus par jour.
- Pour chaque station et chaque heure, afficher une information `X_tram` correspondant au trafic des trams avec des niveaux "faible", "moyen", ou "fort". Faire de même pour les bus.

---

### Exercice 9 - Tri

Hadoop trie les clés des groupes en ordre lexicographique ascendant pendant la phase de shuffling. Modifier la méthode de tri.  
1. Trier les commandes clients du fichier `superstore.csv` par date d’expédition en ordre croissant, puis décroissant.  
2. Trier les clients (identifiant + nom) par profit généré.

---

### Exercice 10 - Requêtes Top-k

Modifier la classe `TopkWordCount.java` pour répondre aux requêtes suivantes :  
1. Les k premières lignes triées par profit (ordre décroissant).  
2. Les k premiers clients en termes de profit réalisé (ordre décroissant).

---

### Exercice 11 - TAM (suite question 9)

Répondre aux questions suivantes :  
- Quelles sont les 10 stations les plus desservies par les trams ?  
- Quelles sont les 10 stations les plus desservies par les bus ?  
- Quelles sont les 10 stations les plus desservies (trams et bus) ?  

---

### Exercice 12 - Taxis New York

Rendez-vous à l'adresse : [trip_record_data](http://www.nyc.gov/html/tlc/html/about/trip_record_data.shtml)  
Télécharger les données des taxis jaunes pour janvier 2018. Répondre aux questions suivantes :  
1. Quelles sont les heures de pointe ?  
2. Quels sont les trois jours les plus chargés du mois ?  
3. Quel est le moyen de paiement le plus utilisé par jour ?  
4. Géolocalisation des zones les plus demandées (départ et arrivée).  
5. La plupart des courses se font-elles avec deux passagers ?  
6. Pourcentage du pourboire par rapport au prix des courses ?  
7. Longueur moyenne des trajets ?  
8. Prix moyen / médian d'une course ?  
9. Nombre de disputes (payment-type=4) par jour ?

---

### Exercice 13 - Jointure par hachage

Une technique classique de jointure en Map/Reduce consiste à découper l'évaluation en sous-tâches exécutables en parallèle.  
Exemple :
```
DimClient(idClient, ...) JOIN FaitVente(idClient, idProduit, ...) JOIN DimProduit(idProduit, ...)
```
Questions :  
1. Comment choisir le nombre de sous-tâches ?  
2. Comment distribuer les données dans les sous-tâches ?
