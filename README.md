# Ai-Notes - Application de Prise de Notes Intelligente

Une application Android moderne et intuitive pour la prise de notes, enrichie de fonctionnalités d'intelligence artificielle pour une expérience utilisateur optimale.

## Fonctionnalités Principales

###  Gestion des Notes
- Création et édition de notes avec une interface intuitive
- Organisation automatique par date de modification
- Sauvegarde automatique des modifications
- Support multilingue (français et anglais etc)
- Recherche instantanée dans les notes
- Suppression sécurisée avec confirmation

###  Résumé Automatique
- Génération de résumés intelligents via l'API Cohere
- Résumé local pour les textes courts
- Extraction des points clés
- Copie rapide des résumés
- Fallback intelligent en cas d'indisponibilité de l'API

###  Reconnaissance Vocale
- Dictée vocale en temps réel
- Support du français
- Insertion intelligente à la position du curseur
- Gestion optimisée de la mémoire
- Permissions sécurisées

###  Synchronisation
- Sauvegarde automatique
- Synchronisation avec le serveur
- Gestion des conflits
- Indicateur de connexion

###  Sécurité
- Gestion sécurisée des données
- Protection contre la perte de données
- Vérification de la connexion
- Gestion des erreurs robuste
- Sauvegarde locale

## Configuration Technique

### Prérequis
- Android 6.0 (API level 23) ou supérieur
- Node.js 14.0 ou supérieur
- PostgreSQL 12.0 ou supérieur
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
Ai-Notes/
├── app/                              # Application Android
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/notes/
│   │       │   ├── adapter/         # Adaptateurs pour les RecyclerViews
│   │       │   ├── api/            # Services API et configuration Retrofit
│   │       │   ├── data/           # Sources de données et repositories
│   │       │   ├── models/         # Modèles de données
│   │       │   ├── navigation/     # Navigation et routing
│   │       │   ├── ui/            # Composants d'interface utilisateur
│   │       │   ├── utils/         # Utilitaires généraux
│   │       │   ├── util/          # Classes utilitaires spécifiques
│   │       │   ├── CreateNoteActivity.java
│   │       │   ├── LoginActivity.java
│   │       │   ├── MainActivity.java
│   │       │   ├── NotesApplication.java
│   │       │   ├── RegisterActivity.java
│   │       │   └── SplashActivity.java
│   │       ├── res/                # Ressources Android
│   │       │   ├── drawable/      # Images et icônes
│   │       │   ├── layout/        # Layouts XML
│   │       │   └── values/        # Strings, couleurs, styles
│   │       └── AndroidManifest.xml
│   └── build.gradle                # Configuration Gradle de l'app
├── backend/                        # Backend Node.js
│   ├── models/                    # Modèles de données
│   ├── routes/                    # Routes API
│   ├── middleware/                # Middleware d'authentification
│   ├── server.js                  # Point d'entrée du serveur
│   ├── config/                    # Configuration de la base de données
│   │   └── database.js           # Configuration PostgreSQL
│   └── migrations/               # Migrations de la base de données
├── gradle/                        # Wrapper Gradle
├── .gradle/                       # Cache Gradle
├── .idea/                         # Configuration Android Studio
├── build/                         # Dossier de build
├── build.gradle.kts              # Configuration Gradle du projet
├── settings.gradle.kts           # Paramètres Gradle
├── gradle.properties             # Propriétés Gradle
├── local.properties              # Configuration locale
└── README.md                     # Documentation
```

### Composants Principaux

#### Frontend (Android)
- **Activities** : Interface utilisateur principale
  - `MainActivity` : Liste des notes
  - `CreateNoteActivity` : Création/édition de notes
  - `LoginActivity` : Authentification
  - `RegisterActivity` : Inscription
  - `SplashActivity` : Écran de démarrage

- **API** : Communication avec le backend
  - Services REST
  - Configuration Retrofit
  - Gestion des erreurs

- **Models** : Structure des données
  - Note
  - User
  - Autres modèles de données

- **UI Components** : Composants réutilisables
  - Adaptateurs pour les listes
  - Composants de navigation
  - Vues personnalisées

#### Backend (Node.js)
- **Server** : Serveur Express.js
  - Gestion des requêtes HTTP
  - Middleware d'authentification
  - Routes API

- **Database** : PostgreSQL
  - Configuration Sequelize ORM
  - Migrations automatisées
  - Schéma optimisé
  - Tables :
    - users (id, email, password_hash, created_at, updated_at)
    - notes (id, title, content, user_id, created_at, updated_at)
    - summaries (id, note_id, content, created_at)
  - Relations et contraintes d'intégrité
  - Indexation optimisée

- **API Endpoints**
  - Authentification
  - CRUD des notes
  - Gestion des utilisateurs

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


## Contact

Pour toute question ou suggestion, veuillez ouvrir une issue sur GitHub.
