package config.tenants.XM.uaa.lep.security.oauth2

import com.icthh.xm.uaa.security.oauth2.LepTokenGranter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.TokenRequest

TokenRequest tokenRequest = lepContext.inArgs.tokenRequest
LepTokenGranter lepTokenGranter = lepContext.services.lepTokenGranter
def clientDetailsService = lepContext.services.clientDetailsService

String clientId = tokenRequest.getClientId()
ClientDetails client = clientDetailsService.loadClientByClientId(clientId)

def authenticationManager = lepTokenGranter.getAuthenticationManager()
def tokenService = lepTokenGranter.getTokenService()

def username = tokenRequest.getRequestParameters().username
def password = tokenRequest.getRequestParameters().password

def usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password)
def request = tokenRequest.createOAuth2Request(client)
def authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken)

return tokenService.createAccessToken(new OAuth2Authentication(request, authenticate))
