# MatchFreelance - Demonstration Guide for Professor

## Project Overview
**MatchFreelance** is a microservices-based freelancing platform built with Spring Boot and Angular.

---

## 🏗️ Architecture Components

### 1. Infrastructure Services
- **Eureka Server** (Port 8761) - Service Registry & Discovery
- **Config Server** (Port 8888) - Centralized Configuration
- **API Gateway** (Port 8081) - Single Entry Point & Load Balancer

### 2. Business Microservices
- **User Service** (Port 8085) - User management
- **Project Service** (Port 8082) - Project CRUD
- **Candidature Service** (Port 8083) - Job applications
- **Contract Service** (Port 8084) - Contract management with AI features
- **Analytics Service** (Port 8095) - Analytics & reporting

### 3. Frontend
- **Angular Application** (Port 4200) - User interface

---

## 📋 Demonstration Checklist

### Part 1: Show Running Services (5 minutes)

#### 1.1 Eureka Dashboard
**URL:** http://localhost:8761

**What to show:**
- All registered services (should see 5+ services)
- Service instances with status UP
- Explain: "This is our service registry. All microservices register here automatically."

#### 1.2 Config Server
**URL:** http://localhost:8888/contract-service/default

**What to show:**
- JSON response with configuration
- Point out `propertySources` array
- Explain: "Centralized configuration for all services. Changes here affect all instances."

**Command to show in terminal:**
```bash
curl http://localhost:8888/contract-service/default
```

#### 1.3 API Gateway
**URL:** http://localhost:8081/actuator/health

**What to show:**
- Gateway health status
- Explain: "Single entry point for all client requests. Routes to appropriate services."

---

### Part 2: Demonstrate Microservices Communication (10 minutes)

#### 2.1 Service Discovery with Eureka
**Show in code:**
```
contract-service/src/main/java/com/freelancing/contract/client/ProjectClient.java
```

**Explain:**
- OpenFeign client with `@FeignClient(name = "project-service")`
- No hardcoded URLs - uses Eureka for service discovery
- Automatic load balancing

**Live Demo:**
1. Open browser: http://localhost:4200
2. Navigate to a contract
3. Show that contract displays project information
4. Explain: "Contract service called Project service via Eureka - no direct URL needed"

#### 2.2 Load Balancing
**Show in terminal:**
```bash
# Check Eureka registry
curl http://localhost:8761/eureka/apps | grep -i "contract-service"
```

**Explain:**
- Multiple instances can run on different ports
- Gateway automatically distributes load
- If one instance fails, traffic goes to healthy instances

---

### Part 3: Show Key Features (10 minutes)

#### 3.1 AI-Powered Contract Features
**URL:** http://localhost:4200 (navigate to contracts)

**Features to demonstrate:**
1. **AI Contract Briefing** - Uses Ollama (local LLM)
   - Click on a contract
   - Show AI-generated briefing
   - Explain: "Uses llama3.2:1b model running locally"

2. **Contract Preview Generation**
   - Show generated contract preview
   - Explain: "AI generates HTML/CSS based on contract data"

**Show in code:**
```
contract-service/src/main/resources/application.properties
```
Point out:
```properties
contract.ai.ollama.enabled=true
contract.ai.ollama.model=llama3.2:1b
```

#### 3.2 Financial Calculations
**Show in browser:**
- Navigate to contract financial summary
- Show payment milestones
- Platform fee calculation (10%)

**Show in code:**
```
contract-service/src/main/java/com/freelancing/contract/service/ContractFinancialService.java
```

---

### Part 4: Unit Tests (5 minutes)

#### 4.1 Run Financial Service Tests
**Command:**
```bash
cd contract-service
mvn test -Dtest=ContractFinancialServiceTest
```

**What to show:**
- 8 tests passing
- Explain test coverage:
  - Financial calculations
  - Payment milestones
  - Platform fee logic
  - Edge cases (0%, 100% progress)

**Show test file:**
```
contract-service/src/test/java/com/freelancing/contract/service/ContractFinancialServiceTest.java
```

#### 4.2 Angular Tests
**Show files:**
```
angular/src/app/services/contract.service.spec.ts
angular/src/app/services/project.service.spec.ts
```

**Explain:**
- HTTP client testing
- Mock responses
- Service integration tests

---

### Part 5: Configuration Management (5 minutes)

#### 5.1 Show Config Server Repository
**Path:** `config-server/src/main/resources/config-repo/`

**Files to show:**
- `application.yml` - Shared configuration
- `contract-service.yml` - Service-specific config
- `api-gateway.yml` - Gateway routes

#### 5.2 Demonstrate Configuration Refresh
**Show in code:**
```
contract-service/src/main/resources/application.properties
```

```properties
spring.config.import=optional:configserver:http://localhost:8888
```

**Explain:**
- `optional:` means service starts even if Config Server is down
- Fallback to local configuration
- Can be updated without redeploying services

---

### Part 6: API Gateway Routing (5 minutes)

#### 6.1 Show Gateway Configuration
**File:** `config-server/src/main/resources/config-repo/api-gateway.yml`

**Explain routing:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: contract-service
          uri: lb://contract-service
          predicates:
            - Path=/api/contracts/**
```

**Live Demo:**
```bash
# Direct service call (bypassing gateway)
curl http://localhost:8084/api/contracts

# Through gateway (recommended)
curl http://localhost:8081/api/contracts
```

#### 6.2 Show Angular Proxy
**File:** `angular/proxy.conf.js`

```javascript
{
  context: ['/api'],
  target: 'http://localhost:8081',  // Points to API Gateway
  secure: false,
  changeOrigin: true
}
```

**Explain:**
- Angular dev server proxies `/api/*` to Gateway
- Single point of entry
- CORS handling

---

## 🎯 Key Points to Emphasize

### 1. Microservices Architecture
✅ **Independent services** - Each can be deployed separately
✅ **Service discovery** - No hardcoded URLs
✅ **Load balancing** - Automatic traffic distribution
✅ **Fault tolerance** - Services continue if one fails

### 2. Spring Cloud Features
✅ **Eureka** - Service registry
✅ **Config Server** - Centralized configuration
✅ **OpenFeign** - Declarative REST clients
✅ **API Gateway** - Routing & load balancing

### 3. Modern Technologies
✅ **Spring Boot 3.2.2** - Latest framework
✅ **Angular** - Modern frontend
✅ **Ollama** - Local AI integration
✅ **JUnit 5 & Mockito** - Testing

### 4. Best Practices
✅ **Unit tests** - Service layer testing
✅ **DTOs** - Clean data transfer
✅ **Repository pattern** - Data access
✅ **RESTful APIs** - Standard endpoints

---

## 🚀 Quick Start Commands

### Start All Services:
```bash
# 1. Start Eureka Server
cd eureka-server && mvn spring-boot:run

# 2. Start Config Server
cd config-server && mvn spring-boot:run

# 3. Start Business Services
cd user-service && mvn spring-boot:run
cd project-service && mvn spring-boot:run
cd candidature-service && mvn spring-boot:run
cd contract-service && mvn spring-boot:run
cd analytics-service && mvn spring-boot:run

# 4. Start API Gateway
cd api-gateway && mvn spring-boot:run

# 5. Start Angular
cd angular && npm start
```

### Run Tests:
```bash
# Backend tests
cd contract-service
mvn test -Dtest=ContractFinancialServiceTest

# Frontend tests
cd angular
npm test
```

---

## 📊 URLs Reference

| Service | URL | Purpose |
|---------|-----|---------|
| **Eureka Dashboard** | http://localhost:8761 | View all registered services |
| **Config Server** | http://localhost:8888 | Configuration management |
| **API Gateway** | http://localhost:8081 | Single entry point |
| **Angular App** | http://localhost:4200 | User interface |
| **Contract Service** | http://localhost:8084 | Direct service access |
| **Project Service** | http://localhost:8082 | Direct service access |
| **User Service** | http://localhost:8085 | Direct service access |

---

## 🎬 Demonstration Flow (30 minutes)

1. **Introduction** (2 min) - Architecture overview
2. **Eureka Dashboard** (3 min) - Show service registry
3. **Config Server** (3 min) - Centralized configuration
4. **Live Application** (5 min) - Browse features
5. **Service Communication** (5 min) - Show Feign clients
6. **AI Features** (5 min) - Contract briefing & preview
7. **Unit Tests** (3 min) - Run and explain tests
8. **API Gateway** (2 min) - Routing demonstration
9. **Q&A** (2 min) - Answer questions

---

## 💡 Troubleshooting Tips

### If a service won't start:
```bash
# Check if port is in use
netstat -ano | findstr :8084

# Kill process if needed
taskkill /PID <process_id> /F
```

### If Eureka shows no services:
- Wait 30 seconds for registration
- Check service logs for errors
- Verify `eureka.client.service-url.defaultZone` is correct

### If Config Server fails:
- Services will use local `application.properties`
- Check Config Server logs
- Verify config-repo files exist

---

## 📝 Questions Professor Might Ask

**Q: Why use microservices instead of monolith?**
A: Independent deployment, scalability, technology flexibility, fault isolation

**Q: How do services find each other?**
A: Eureka service registry - services register on startup, clients query Eureka

**Q: What if Eureka goes down?**
A: Services cache registry locally, continue working with last known locations

**Q: Why use Config Server?**
A: Centralized configuration, environment-specific configs, no redeployment needed

**Q: How does load balancing work?**
A: Spring Cloud LoadBalancer with Eureka - round-robin by default

**Q: What about security?**
A: Can add Spring Security, OAuth2, JWT tokens (not implemented in this demo)

**Q: How do you handle transactions across services?**
A: Saga pattern or eventual consistency (each service has own database)

**Q: Why OpenFeign instead of RestTemplate?**
A: Declarative, less boilerplate, built-in Eureka integration, easier to test

---

## ✅ Final Checklist Before Demo

- [ ] All services running (check with `listProcesses`)
- [ ] Eureka shows all services as UP
- [ ] Config Server responding
- [ ] Angular app loads
- [ ] Can navigate and view contracts
- [ ] AI features working (Ollama running)
- [ ] Tests pass
- [ ] Browser tabs ready:
  - [ ] http://localhost:8761 (Eureka)
  - [ ] http://localhost:8888/contract-service/default (Config)
  - [ ] http://localhost:4200 (App)
- [ ] Code files ready to show
- [ ] Terminal ready for commands

---

**Good luck with your demonstration! 🎓**
