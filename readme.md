# Java Chat Application

A real-time chat application built with Java Swing, supporting instant messaging and contact management.

## Features

- User authentication (login/register)
- Real-time messaging
- Contact management
- Offline message storage
- Online status tracking
- Message history
- Performance monitoring

## Prerequisites

- Java JDK 8 or higher
- JFreeChart library for performance visualization
- Network connectivity for client-server communication

## Installation

1. Clone the repository
2. Compile the source files:
```bash
javac -d bin src/**/*.java
```
3. Start the server:
```bash
java -cp bin ServerSystem.Server
```
4. Launch the client:
```bash
java -cp bin UserInterface.ChatApp
```

## System Architecture

- Server: Handles message routing and user management
- Client: Provides GUI and manages user interactions
- Message System: Facilitates communication protocol
- User Management: Handles authentication and user data

## Usage

### Server
- Default port: 8888
- Supports multiple client connections
- Stores user data and offline messages

### Client
1. Register a new account or login
2. Add contacts through search
3. Click on a contact to start chatting
4. Messages are delivered in real-time when both users are online
5. Offline messages are delivered upon next login

## Error Handling

- Connection loss recovery
- Invalid message detection
- Data corruption prevention
- User authentication validation

## Security Features

- Password validation
- User session management
- Data persistence security
- Input validation

## Performance

- Message delay monitoring
- Real-time status updates
- Efficient message routing
- Background thread management

## Known Limitations

- Text-only messages

- Single server architecture

- Local storage for user data

- Basic authentication system

  
