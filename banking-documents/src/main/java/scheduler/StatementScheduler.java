package scheduler;


import entity.Account;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Singleton
@Startup
public class StatementScheduler {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Inject
    private JMSContext jmsContext; // Use CDI to inject the JMSContext

    @Resource(lookup = "jms/statementQueue") // Find the queue we created
    private Queue statementQueue;

    // Run at 4 AM on the 1st day of every month.
    @Schedule(dayOfMonth = "1", hour = "4", minute = "0", persistent = true)
    public void triggerMonthlyStatements() {
        System.out.println("STATEMENT SCHEDULER: Starting monthly statement generation job...");

        // Determine the date range for last month's statement
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = lastMonth.atDay(1);
        LocalDate endDate = lastMonth.atEndOfMonth();

        // Find all accounts that had transactions last month
        List<Account> accountsForStatement = findAccountsWithActivity(startDate, endDate);

        System.out.println("Found " + accountsForStatement.size() + " accounts to generate statements for.");

        for (Account account : accountsForStatement) {
            // Create a simple message payload
            String message = account.getAccountNumber() + ";" + startDate + ";" + endDate;

            // Send a message to the JMS queue for each account
            jmsContext.createProducer().send(statementQueue, message);
            System.out.println("  -> Queued statement job for account: " + account.getAccountNumber());
        }

        System.out.println("STATEMENT SCHEDULER: All statement jobs have been queued.");
    }

    private List<Account> findAccountsWithActivity(LocalDate startDate, LocalDate endDate) {
        // This query finds distinct accounts that appear as either from or to in the transaction log
        return em.createQuery(
                        "SELECT DISTINCT a FROM Transaction t JOIN t.fromAccount a WHERE t.transactionDate >= :start AND t.transactionDate < :end " +
                                "UNION SELECT DISTINCT b FROM Transaction t JOIN t.toAccount b WHERE t.transactionDate >= :start AND t.transactionDate < :end", Account.class)
                .setParameter("start", startDate.atStartOfDay())
                .setParameter("end", endDate.plusDays(1).atStartOfDay())
                .getResultList();
    }
}