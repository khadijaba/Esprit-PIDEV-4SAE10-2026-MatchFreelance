# Freelancing Platform - BackOffice

Spring Boot REST API for managing projects in a freelancing platform.

## Project Structure

```
spring/
├── src/
│   └── main/
│       ├── java/com/freelancing/backoffice/
│       │   ├── BackofficeApplication.java       # Main application class
│       │   ├── controller/
│       │   │   └── ProjectController.java       # REST API endpoints
│       │   ├── service/
│       │   │   └── ProjectService.java          # Business logic
│       │   ├── repository/
│       │   │   └── ProjectRepository.java       # Data access layer
│       │   ├── entity/
│       │   │   └── Project.java                 # JPA entity
│       │   ├── dto/
│       │   │   ├── ProjectRequestDTO.java       # Request DTO
│       │   │   └── ProjectResponseDTO.java      # Response DTO
│       │   └── enums/
│       │       └── ProjectStatus.java           # Project status enum
│       └── resources/
│           └── application.properties           # Configuration
└── pom.xml                                      # Maven dependencies
```

## Features

- ✅ Full CRUD operations for Projects
- ✅ Search projects by title
- ✅ Filter projects by status
- ✅ Input validation
- ✅ RESTful API design
- ✅ H2 in-memory database (development)
- ✅ MySQL support (production)

## API Endpoints

### Get All Projects
```
GET /api/projects
```

### Get Project by ID
```
GET /api/projects/{id}
```

### Get Projects by Status
```
GET /api/projects/status/{status}
```
Status values: `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### Search Projects by Title
```
GET /api/projects/search?title={title}
```

### Create Project
```
POST /api/projects
Content-Type: application/json

{
  "title": "Mobile App Development",
  "description": "Need a React Native developer",
  "budget": 5000.0,
  "duration": 30
}
```

### Update Project
```
PUT /api/projects/{id}
Content-Type: application/json

{
  "title": "Mobile App Development",
  "description": "Updated description",
  "budget": 6000.0,
  "duration": 45,
  "status": "IN_PROGRESS"
}
```

### Delete Project
```
DELETE /api/projects/{id}
```

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Steps

1. **Navigate to project directory**
```bash
cd c:\Users\azizb\Desktop\spring
```

2. **Build the project**
```bash
mvn clean install
```

3. **Run the application**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access H2 Console
Visit `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:freelancing`
- Username: `sa`
- Password: (leave blank)

## Testing with cURL

### Create a project
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Website Development\",\"description\":\"Build a corporate website\",\"budget\":3000.0,\"duration\":20}"
```

### Get all projects
```bash
curl http://localhost:8080/api/projects
```

### Get project by ID
```bash
curl http://localhost:8080/api/projects/1
```

### Update a project
```bash
curl -X PUT http://localhost:8080/api/projects/1 \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Website Development Updated\",\"description\":\"Updated description\",\"budget\":3500.0,\"duration\":25,\"status\":\"IN_PROGRESS\"}"
```

### Delete a project
```bash
curl -X DELETE http://localhost:8080/api/projects/1
```

## Database Configuration

### Development (H2)
The application uses H2 in-memory database by default. Data is not persisted between restarts.

### Production (MySQL)
To use MySQL, uncomment the MySQL configuration in `application.properties` and comment out the H2 configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/freelancing
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

## Technologies Used

- **Spring Boot 3.2.2** - Application framework
- **Spring Data JPA** - Data persistence
- **Spring Web** - REST API
- **H2 Database** - In-memory database (dev)
- **MySQL** - Relational database (prod)
- **Lombok** - Boilerplate code reduction
- **Jakarta Validation** - Input validation
- **Maven** - Build tool

## Project Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key (auto-generated) |
| title | String | Project title (required, 3-255 chars) |
| description | String | Project description (required, max 2000 chars) |
| budget | Double | Project budget (required, positive) |
| duration | Integer | Project duration in days (required, positive) |
| createdAt | Date | Creation timestamp (auto-generated) |
| status | ProjectStatus | Project status (default: OPEN) |

## License

MIT
