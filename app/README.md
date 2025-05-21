# Notes - Application de Prise de Notes Intelligente

Une application Android moderne et intuitive pour la prise de notes, enrichie de fonctionnalités d'intelligence artificielle pour une expérience utilisateur optimale.

## Fonctionnalités Principales

### 📝 Gestion des Notes
- Création et édition de notes avec une interface intuitive
- Organisation automatique par date de modification
- Sauvegarde automatique des modifications
- Support multilingue (français et anglais)
- Recherche instantanée dans les notes
- Suppression sécurisée avec confirmation

### 🎯 Résumé Automatique
- Génération de résumés intelligents via l'API Cohere
- Résumé local pour les textes courts
- Extraction des points clés
- Copie rapide des résumés
- Fallback intelligent en cas d'indisponibilité de l'API

### 🎤 Reconnaissance Vocale
- Dictée vocale en temps réel
- Support du français
- Insertion intelligente à la position du curseur
- Gestion optimisée de la mémoire
- Permissions sécurisées

### 💾 Synchronisation
- Sauvegarde automatique
- Gestion hors ligne
- Synchronisation avec le serveur
- Gestion des conflits
- Indicateur de connexion

### 🔒 Sécurité
- Gestion sécurisée des données
- Protection contre la perte de données
- Vérification de la connexion
- Gestion des erreurs robuste
- Sauvegarde locale

## Configuration Technique

### Prérequis
- Android 6.0 (API level 23) ou supérieur
- Connexion Internet pour les fonctionnalités en ligne
- Microphone pour la reconnaissance vocale
- Autorisations :
  - Internet
  - Microphone
  - Stockage

### Dépendances Principales
```gradle
dependencies {
    // AndroidX Core
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
    
    // Material Design
    implementation 'com.google.android.material:material:1.11.0'
    
    // Retrofit pour les appels API
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp pour les requêtes réseau
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Gson pour la sérialisation
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

### API Externes
- Cohere API pour la génération de résumés
- Google Speech Recognition pour la dictée vocale

## Architecture

### Structure du Projet
```
app/
├── java/
│   └── com.example.notes/
│       ├── activities/
│       │   ├── MainActivity.java
│       │   └── CreateNoteActivity.java
│       ├── api/
│       │   ├── ApiService.java
│       │   └── RetrofitClient.java
│       ├── models/
│       │   └── Note.java
│       └── utils/
│           └── NetworkUtils.java
└── res/
    ├── layout/
    ├── values/
    └── drawable/
```

### Composants Principaux
- **Activities** : Gestion de l'interface utilisateur
- **API Service** : Communication avec le backend
- **Models** : Structure des données
- **Utils** : Fonctions utilitaires

## Fonctionnalités Détaillées

### Système de Résumé
1. **Résumé Local**
   - Analyse des phrases clés
   - Extraction intelligente
   - Gestion des textes courts
   - Optimisation de la performance

2. **Résumé API**
   - Intégration Cohere
   - Paramètres optimisés
   - Gestion des erreurs
   - Fallback automatique

### Système de Dictée
1. **Configuration**
   - Initialisation du service
   - Gestion des permissions
   - Configuration de la langue

2. **Traitement**
   - Capture audio
   - Reconnaissance en temps réel
   - Insertion contextuelle
   - Gestion des erreurs

### Gestion des Notes
1. **Création**
   - Interface intuitive
   - Sauvegarde automatique
   - Validation des données
   - Gestion des erreurs

2. **Édition**
   - Édition en temps réel
   - Historique des modifications
   - Annulation des changements
   - Synchronisation

3. **Suppression**
   - Confirmation de sécurité
   - Suppression sécurisée
   - Nettoyage des ressources
   - Synchronisation

## Maintenance et Support

### Gestion des Erreurs
- Logs détaillés
- Messages utilisateur clairs
- Récupération automatique
- Rapports d'erreur

### Performance
- Optimisation de la mémoire
- Gestion du cache
- Chargement asynchrone
- Mise en cache intelligente

### Sécurité
- Validation des entrées
- Protection des données
- Gestion des sessions
- Mise à jour sécurisée

## Contribution

Les contributions sont les bienvenues ! Veuillez suivre ces étapes :
1. Fork le projet
2. Créer une branche pour votre fonctionnalité
3. Commiter vos changements
4. Pousser vers la branche
5. Ouvrir une Pull Request

## Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## Contact

Pour toute question ou suggestion, veuillez ouvrir une issue sur GitHub. 