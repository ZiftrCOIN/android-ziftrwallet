-------------------------
Activation table
-------------------------
_id
- Primary Key
- INT
- AutoInc
coin_type
- Text
- Not NUll
status (Activated?)
- INT

Per Coin
-------------------------
Receive table
-------------------------
_id
- INT
- Autoinc
priv_key
- Text
- Unique
- Not NUll
pub_key
- Text
- Unique
- Not Null
address
- Text
- Unique
- Not Null
note
- Text
balance (last known)
- INT
creation_timestamp
- INT
modified_timestamp
- INT
hidden
- INT
spent_From
- INT
-------------------------
Transaction table
-------------------------
_id
- Primary Key
- INT
- Autoinc
hash
- Text
- Unique
- Not Null
amount
- INT
fee
- INT
note
- Text
time
- INT
num_Confirmations
- INT
-------------------------
Send table
-------------------------
_id
- Primary Key
- INT
- Autoinc
address
- Text
- Unique
- Not Null
note
- Text
balance
- INT
modified_timestamp (Last known time address used in txn)
- INT