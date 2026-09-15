# Why We Use DTO (Data Transfer Object)

Data Transfer Objects are an essential part of modern backend development
They act as a **bridge** between the client and the server, ensuring security, performance and maintainability

---
## 🔒 Security
- DTOs prevent exposing sensitive information from database entities
- For example the user entity (profile)contains a 'passwordHash' field, but our 'AuthResponseDTO' only sends back a JWT and a phone number.


## Flexibility and Decoupling
- DTO's separate the API contract(what the client sees) from the database schema(how data is stored)
- If the database changes, the API doesn't necessarily need to change, as long as DTOs remain the same

## ⚡ Performance
- DTO's send only the required fields over the network.
- This avoids transferring unnecessary data, which is especially important for mobile or low-bandwidth clients

## ✅ Validation
- They are ideal place for request validation using annotations like @NotBlank etc.
- This ensures incoming data is validated before reaching service or repository layers
