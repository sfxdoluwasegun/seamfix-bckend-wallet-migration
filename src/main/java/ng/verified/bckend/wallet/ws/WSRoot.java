package ng.verified.bckend.wallet.ws;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import ng.verified.bckend.wallet.filters.AuthenticationFilter;
import ng.verified.bckend.wallet.services.WalletService;

@ApplicationPath(value = "/")
public class WSRoot extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		// TODO Auto-generated method stub
		
		Set<Class<?>> services = new HashSet<>();
		services.add(WalletService.class);
		services.add(AuthenticationFilter.class);
		
		return services;
	}

}
