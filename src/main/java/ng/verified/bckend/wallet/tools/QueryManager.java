package ng.verified.bckend.wallet.tools;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import ng.verified.bckend.wallet.jbeans.TransactionLogs;
import ng.verified.jpa.WalletStatement;
import ng.verified.jpa.WalletStatement_;
import ng.verified.jpa.Wrapper;
import ng.verified.jpa.Wrapper_;
import ng.verified.jpa.tools.QueryService;

@Stateless
public class QueryManager extends QueryService {
	
	/**
	 * Fetch {@link WalletStatement} by {@link Wrapper} relationship.
	 * 
	 * @param wrapper
	 * @return list
	 */
	public List<TransactionLogs> getTransactionLogsByWrapper(Wrapper wrapper) {
		// TODO Auto-generated method stub
		
		CriteriaQuery<TransactionLogs> criteriaQuery = criteriaBuilder.createQuery(TransactionLogs.class);
		Root<WalletStatement> root = criteriaQuery.from(WalletStatement.class);
		
		Join<WalletStatement, Wrapper> join = root.join(WalletStatement_.wrapper);
		
		criteriaQuery.select(criteriaBuilder.construct(TransactionLogs.class, 
				join.get(Wrapper_.name).alias("serviceName"), 
				criteriaBuilder.toBigDecimal(criteriaBuilder.sum(root.get(WalletStatement_.amount))).alias("cost"), 
				root.get(WalletStatement_.referenceNo).alias("referenceNo"),
				criteriaBuilder.greatest(root.get(WalletStatement_.timestamp)).alias("timestamp")
				));
		criteriaQuery.where(criteriaBuilder.and(
				criteriaBuilder.equal(join.get(Wrapper_.pk), wrapper.getPk()), 
				criteriaBuilder.equal(root.get(WalletStatement_.deleted), false)
				));
		criteriaQuery.groupBy(
				join.get(Wrapper_.name), 
				root.get(WalletStatement_.amount), 
				root.get(WalletStatement_.referenceNo), 
				root.get(WalletStatement_.timestamp)
				);

		try {
			return entityManager.createQuery(criteriaQuery).getResultList();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.warn("No WalletStatement exists for wrapper:" + wrapper.getPk());
		}

		return null;
	}

}
