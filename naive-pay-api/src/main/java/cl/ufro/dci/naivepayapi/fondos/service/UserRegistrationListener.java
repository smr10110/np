package cl.ufro.dci.naivepayapi.fondos.service;

import cl.ufro.dci.naivepayapi.registro.service.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for automatic account creation on user registration.
 * <p>
 * This component listens to user registration events from the Registration module
 * and automatically creates an associated account in the Funds module, ensuring
 * that every new user has an account ready to use.
 * </p>
 * 
 * <p><b>Integration pattern:</b> Event-driven architecture</p>
 * <ul>
 *   <li>Decouples the Registration module from the Funds module</li>
 *   <li>Ensures automatic and consistent account creation</li>
 *   <li>Prevents race conditions with duplicate account checks</li>
 * </ul>
 * 
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>Listens for {@link UserRegisteredEvent} events</li>
 *   <li>Checks if an account already exists for the user</li>
 *   <li>Creates a new account with initial balance of 0 if it doesn't exist</li>
 *   <li>Logs the account creation for auditing purposes</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 * @see AccountService
 * @see UserRegisteredEvent
 */
@Component
public class UserRegistrationListener {

    private final AccountService accountService;

    /**
     * Constructor with dependency injection.
     * 
     * @param accountService the account management service
     */
    public UserRegistrationListener(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Handles user registration events and creates an associated account.
     * <p>
     * This method is automatically invoked when a {@link UserRegisteredEvent}
     * is published in the application context. It ensures that every new user
     * has an account created in the funds system.
     * </p>
     * 
     * <p><b>Process flow:</b></p>
     * <ol>
     *   <li>Receives the user registration event</li>
     *   <li>Extracts the user ID from the event</li>
     *   <li>Checks if an account already exists for this user</li>
     *   <li>Creates a new account if it doesn't exist</li>
     *   <li>Logs the successful account creation</li>
     * </ol>
     * 
     * <p><b>Note:</b> The duplicate check prevents errors if the event
     * is processed multiple times.</p>
     * 
     * @param event the user registration event containing the new user's ID
     */
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        Long userId = event.getUserId();

        if (!accountService.accountExists(userId)) {
            accountService.createAccount(userId);
            System.out.println("Account created for userId: " + userId);
        }
    }
}