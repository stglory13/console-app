INSERT INTO account (id, guid, name, maximal_overdraft, current_balance) VALUES
    (1, '8d9d35e2-15b3-4fad-b853-f5731e9e19fa', 'TestAccount No 1', 1000, 913.13),
    (2, 'd1e39c65-48c9-42ef-9c50-8dd5a072e510', 'TestAccount No 2',  500, 1513.50),
    (3, '6eb7e588-5d85-4285-8c64-3be32a70393b', 'TestAccount No 3',  200,  700.00);

SELECT setval('account_seq', 100);
