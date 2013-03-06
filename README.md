# Nuxeo YouTube Integration

## Create an OAuth2 Service Provider
Name: YouTubePublisher
clientId = <your client id>
clientSecret = <your client secret>
AUTHORIZATION_SERVER_URL = "https://accounts.google.com/o/oauth2/auth"
TOKEN_SERVER_URL = "https://accounts.google.com/o/oauth2/token"
SCOPES = https://www.googleapis.com/auth/youtube


### OAuth 2

If you're using the installed applications instead of the service account you need to start the authorization flow by going to:

https://accounts.google.com/o/oauth2/auth?client_id=CLIENT_ID&redirect_uri=http://localhost:8080/nuxeo/site/oauth2/YouTube/callback?app%3Dtrue&response_type=code&scope=https://www.googleapis.com/auth/youtube
