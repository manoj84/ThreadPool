package core;

import java.util.UUID;

/**
 * Abstract resource class R with unique ID's
 * 
 * @author manoj
 * 
 */
public abstract class R {
	private UUID id = null;

	public UUID getId() {
		return id;
	}

	public R() {
		id = UUID.randomUUID();
	}
}
