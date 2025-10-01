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


# NGười dùng quên mk và yêu cầu reset mk
POST http://localhost:8081/auth/forgot-password

  {
  "email": "lehbac05@gmail.com",
  }

POST http://localhost:8081/auth/verify-otp

{
  "email": "lehbac05@gmail.com",
  "otp": "654713"
} 

POST http://localhost:8081/auth//reset-password

{
  "email": "lehbac05@gmail.com",
  "reset_token": "HgYvhHNDAm1f9nawYxKYrontS2_VbH7fMuZS4ey48go",
  "newPassword": "123456"
}

