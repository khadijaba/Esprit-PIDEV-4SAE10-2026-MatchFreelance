# Eureka centrale – configuration pour toute l’équipe

## Serveur Eureka (une seule machine, ex. 192.168.1.8)

Fichier : **Eureka/eureka/src/main/resources/application.properties**

```properties
spring.application.name=eureka
server.port=8761

eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

eureka.instance.hostname=192.168.1.8
eureka.instance.prefer-ip-address=true
```

- **192.168.1.8** = IP de la machine qui lance Eureka (à adapter si besoin).
- Les microservices et la Gateway doivent tous utiliser l’URL **http://192.168.1.8:8761/eureka/**.

---

## Tous les autres membres (microservices + Gateway)

Chaque membre configure **son** microservice (et la Gateway si elle est partagée) pour pointer vers **cette** Eureka.

### Option 1 : Variable d’environnement (recommandé)

Dans **application.properties** (inchangé) :

```properties
eureka.client.service-url.defaultZone=${EUREKA_URL:http://localhost:8761/eureka/}
eureka.instance.prefer-ip-address=true
```

Au lancement, chaque membre définit l’URL de l’Eureka centrale :

- **Windows (CMD)** : `set EUREKA_URL=http://192.168.1.8:8761/eureka/`
- **Windows (PowerShell)** : `$env:EUREKA_URL="http://192.168.1.8:8761/eureka/"`
- **IDE** : dans la config de run, ajouter la variable d’environnement **EUREKA_URL** = **http://192.168.1.8:8761/eureka/**

Comme ça, tout le monde pointe vers **192.168.1.8** sans modifier le fichier.

### Option 2 : URL en dur dans le fichier (pour l’équipe)

Dans **application.properties** de chaque microservice et de la Gateway :

```properties
# Eureka centrale (IP de la machine qui héberge Eureka)
eureka.client.service-url.defaultZone=http://192.168.1.8:8761/eureka/
eureka.instance.prefer-ip-address=true
```

Remplace **192.168.1.8** par l’IP réelle de la machine où Eureka tourne.

---

## Récapitulatif

| Qui              | Où                    | Config Eureka |
|------------------|-----------------------|----------------|
| Eureka (central) | 192.168.1.8           | `hostname=192.168.1.8`, port 8761 |
| Tous les autres  | Microservices + Gateway | `defaultZone=http://192.168.1.8:8761/eureka/` (ou `EUREKA_URL`) |

Oui : les autres membres utilisent bien **eureka.client.service-url.defaultZone** et **eureka.instance.prefer-ip-address=true**, avec l’URL **http://192.168.1.8:8761/eureka/** (ou la variable **EUREKA_URL** égale à cette valeur).
