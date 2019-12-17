CREATE TABLE `users` (
  id BIGINT AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL,
  password  VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO `users` (`username`, `password`) VALUES ('starter', '$2a$10$qt0IqRPLQ.aB.tJS38/1hOHXpL68NjACk4pFWFHYdlaAZJaFjzUdq');
