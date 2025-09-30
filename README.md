# ALM-Team-Tim-BE

POST: http://localhost:8081/auth/register
{
  "username": "updateduser5",
  "password": "123",
  "email": "abc5@gmail.com"
}

POST :http://localhost:8081/auth/login
{
  "usernameOrEmail": "updateduser5",
  "password": "123"
}

GET: http://localhost:8081/users

GET: http://localhost:8081/users/{id}

PUT:http://localhost:8081/users/{id}
{
    "username": "user5",
    "password": "123",
    "email": "abc6@gmail.com",
    "phoneNumber": 123456789,
    "profileImage": null,
    "role": null,
    "createdAt": "2025-09-29T17:08:15",
    "passwordChangedAt": null
}

GET http://localhost:8081/profile/users/{id}