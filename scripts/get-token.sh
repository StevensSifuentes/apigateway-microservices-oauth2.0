
curl -s -X POST http://keycloak:8080/realms/orders-inventory-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=" \
  -d "username=" \
  -d "password=" \
  -d "grant_type=password" \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'