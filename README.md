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
  "username": "updateduser4",
    "password": "123",
    "email": "abc4@gmail.com"
}