-- -- V8__add_batch_year.sql
ALTER TABLE users 
ADD COLUMN batch_year INT AFTER phone_number;
