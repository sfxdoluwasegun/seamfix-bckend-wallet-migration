package ng.verified.bckend.wallet.filters;

import java.io.IOException;
import java.security.Key;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {

	private Logger log = Logger.getLogger(getClass());
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// TODO Auto-generated method stub
		
		String authHeaderVal = requestContext.getHeaderString("Authorization");
		String claimsJws = null;

		if(authHeaderVal != null && authHeaderVal.startsWith("Bearer")){
			claimsJws = authHeaderVal.split(" ")[1];
		}else{
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
		
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(Base64.getEncoder().encodeToString("secret".getBytes()));
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, SignatureAlgorithm.HS256.getJcaName());

		try {
			Claims claims = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(claimsJws).getBody();
			String userid = claims.getSubject();
			if (userid == null || userid.isEmpty() || !StringUtils.isNumeric(userid)){
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				return;
			}
			
			requestContext.getHeaders().add("userid", userid);
		} catch (ExpiredJwtException e){
			log.error("ExpiredJwtException");
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		} catch (UnsupportedJwtException | MalformedJwtException | SignatureException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			log.error("JWT Validation failure", e);
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
	}

}
