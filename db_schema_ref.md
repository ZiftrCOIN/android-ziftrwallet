
Activation table
-------------------------
### _id

- Primary Key
- INT
- AutoInc

### coin_type

- Text
- Not NUll

### status (Activated?)

- INT

## blockchain

- INT

| _id | coin_type | status | blockchain | 
| --- | ------- | ---------- | ------- |
|  1  |    BTC  |   0     |  55555  |
|  2  |    LTC  |   1     |  22233  |


Currency Exchange table
-------------------------

### _id

- Primary Key
- INT
- AutoInc

### name

- Text
- Not Null

### currency_to

- Text
- Not Null

### Value

- Text

| _id | name | currency_to | value | 
| --- | ---- | ----------- | ----- |
|  1  |  BTC |   USD       |  360  |
|  2  |  BTC |   EUR       |  222  |


Per Coin
-------------------------
Receive table
-------------------------

### _id

- INT
- Autoinc

### priv_key

- Text
- Unique
- Not NUll

### pub_key

- Text
- Unique
- Not Null

### address

- Text
- Unique
- Not Null

### note

- Text

### balance (last known)

- INT

### creation_timestamp

- INT

### modified_timestamp

- INT

### hidden

- INT

### spent_From

- INT


| _id | priv_key | pub_key | address | label | balance | creation_timestamp | modified_timestamp | hidden | spent_From |
| --- | ---- | ----------- | ----- | ---- | ---- | ---- | ---- | ----- | ----- | 
|  1  |  Ge3c6f11e40f9110b8b3af851cd1a238cb49ad98dc8152ab777ffaa72584d9f5c | 0386a537178b49fc8613e95d688e5b15a338532bcfbdc4bada86bc6a64f4d8444b |  1LcatBeguAtMXDASAL8fxLEVJ7P8bNh4aM | Robert | 0 | 0 | 0 | 1 | 1 |

Transaction table
-------------------------

### _id

- Primary Key
- INT
- Autoinc

### hash

- Text
- Unique
- Not Null

### amount

- INT

### fee

- INT

### note

- Text

### time

- INT

### num_Confirmations

- INT

Send table
-------------------------

### _id

- Primary Key
- INT
- Autoinc

### address

- Text
- Unique
- Not Null

### note

- Text

### balance

- INT

### modified_timestamp (Last known time address used in txn)

- INT


