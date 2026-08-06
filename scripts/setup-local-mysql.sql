-- Ejecutar como root: mysql -u root -p < scripts/setup-local-mysql.sql
CREATE DATABASE IF NOT EXISTS donate
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'donate'@'localhost' IDENTIFIED BY 'secret';
GRANT ALL PRIVILEGES ON donate.* TO 'donate'@'localhost';
FLUSH PRIVILEGES;
