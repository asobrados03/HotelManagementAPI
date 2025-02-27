-- Creación de la base de datos (opcional si ya la crea el contenedor)
CREATE DATABASE IF NOT EXISTS dhhotel;
USE dhhotel;

-- Users Table
CREATE TABLE Users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('CLIENT', 'ADMIN', 'SUPERADMIN') NOT NULL
);

-- Clients Table
CREATE TABLE Client (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- Rooms Table
CREATE TABLE Room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number INT NOT NULL UNIQUE,
    type ENUM('SINGLE', 'DOUBLE', 'SUITE') NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') NOT NULL
);

-- Reservations Table
CREATE TABLE Reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELED') NOT NULL,
    FOREIGN KEY (client_id) REFERENCES Client(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (room_id) REFERENCES Room(id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- Payments Table
CREATE TABLE Payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_date DATE NOT NULL,
    method ENUM('CARD', 'CASH', 'TRANSFER') NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES Reservation(id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- Administrators Table
CREATE TABLE Administrator (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- Insertar usuarios
INSERT INTO Users (email, password, role) VALUES
('client1@example.com', 'hashed_password_1', 'CLIENT'),
('client2@example.com', 'hashed_password_2', 'CLIENT'),
('client3@example.com', 'hashed_password_3', 'CLIENT'),
('admin1@example.com', 'hashed_password_3', 'ADMIN'),
('superadmin@example.com', 'hashed_password_4', 'SUPERADMIN');

-- Insertar clientes (asociados a los usuarios con rol CLIENT)
INSERT INTO Client (user_id, first_name, last_name, phone) VALUES
(1, 'John', 'Doe', '555-5678'),
(2, 'Jane', 'Smith', '555-8765'),
(3, 'Alice', 'Wonderland', '555-0000');

-- Insertar administradores (asociados a los usuarios con rol ADMIN o SUPERADMIN)
INSERT INTO Administrator (user_id, name) VALUES
(4, 'Admin One'),
(5, 'Super Admin');

-- Insert data into Rooms Table
INSERT INTO Room (room_number, type, price_per_night, status)
VALUES
(101, 'SINGLE', 50.00, 'AVAILABLE'),
(102, 'DOUBLE', 80.00, 'OCCUPIED'),
(201, 'SUITE', 120.00, 'MAINTENANCE');

-- Insert data into Reservations Table
INSERT INTO Reservation (client_id, room_id, total_price, start_date, end_date, status)
VALUES
(1, 2, 400.50, '2024-09-01', '2024-09-05', 'CONFIRMED'),
(2, 1, 200.00, '2024-09-10', '2024-09-15', 'PENDING'),
(3, 3, 150.75, '2024-09-05', '2024-09-08', 'CANCELED');

-- Insert data into Payments Table
INSERT INTO Payment (reservation_id, amount, payment_date, method)
VALUES
(1, 400.00, '2024-09-01', 'CARD'),
(2, 250.00, '2024-09-10', 'CASH');
