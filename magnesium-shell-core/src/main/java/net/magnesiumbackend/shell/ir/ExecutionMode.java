package net.magnesiumbackend.shell.ir;

/**
 * Execution mode for commands.
 *
 * <ul>
 *   <li><strong>LOCAL</strong> - Direct handler execution in-process</li>
 *   <li><strong>DATA</strong> - Delegated to Magnesium Data generated APIs</li>
 *   <li><strong>AMQP</strong> - Dispatched as message to broker</li>
 * </ul>
 *
 * <p>Execution mode is part of the Command IR, not runtime logic.</p>
 */
public enum ExecutionMode {

    /**
     * Direct local execution. Handler is invoked directly.
     */
    LOCAL,

    /**
     * Delegated to Magnesium Data layer.
     * Command maps to a generated data access operation.
     */
    DATA,

    /**
     * Dispatched via AMQP broker.
     * Command is published as a message.
     */
    AMQP
}
