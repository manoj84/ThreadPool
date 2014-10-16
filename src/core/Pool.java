package core;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Pool gives you access to reusable resources. Use getPoolInstance to get hold
 * of the singleton instance.
 * 
 * @author manoj
 * 
 */

public final class Pool<T extends R> {
	private static Pool<R> pool = null;
	private boolean isOpen = false;
	private final Queue<R> resources = new LinkedList<R>();

	// This is for unique object identification. I store just the UUID's in
	// theses sets to check for duplicates in O(1) time.

	// An other approach would be to traverse the linked list and find the
	// object, and I would not use sets there.
	// That complexity would be o(n) for add and remove operations.

	// In the space time complexity I am using space to my advantage.
	private final Set<UUID> outgoingResource = new HashSet<UUID>();
	private final Set<UUID> allResource = new HashSet<UUID>();

	/**
	 * Lets make it a singleton.
	 * 
	 * @return Pool<R>
	 */
	public synchronized static Pool<R> getPoolInstance() {
		if (pool == null) {
			pool = new Pool<R>();
			return pool;
		}

		else
			return pool;
	}

	/**
	 * Open the pool.
	 */
	public synchronized void open() {
		isOpen = true;
	}

	/**
	 * Get the total size of the pool.
	 * 
	 * @return total pool size.
	 */
	public synchronized Integer getSize() {
		return allResource.size();
	}

	/**
	 * Check if the pool is open.
	 * 
	 * @return true if pool is open.
	 */
	public boolean isOpen() {
		return this.isOpen;
	}

	/**
	 * Close the pool, but wait till I get back all the resources.
	 */
	public synchronized void close() {
		while (outgoingResource.size() > 0) {
			try {
				System.out.print("Waiting for " + outgoingResource.size()
						+ " resources");
				wait();

			} catch (InterruptedException e) {
				System.out
						.println("Cannot close at the moment, waiting for Resource to be acquired !!");
			}

			finally {
				this.isOpen = false;
			}
		}
	}

	/**
	 * Just close the pool, no wait for resources. All the resources lost.
	 */
	public synchronized void closeNow() {
		this.isOpen = false;
	}

	/**
	 * Add a Resource to the pool. If exists then return true, else false.
	 * 
	 * @param resource
	 * @return true if resource exists.
	 */
	public synchronized boolean add(R resource) {

		if (allResource.contains(resource.getId())) {
			return false;
		}

		else {
			resources.add(resource);
			allResource.add(resource.getId());
			return true;
		}

	}

	public synchronized boolean remove(R resource) throws Exception {

		// Do we manage the resource ??
		if (resource != null && allResource.contains(resource.getId())) {
			// May be the resource is out.
			if (outgoingResource.contains(resource.getId())) {
				while (!outgoingResource.contains(resource.getId())) {
					try {
						wait();

					} catch (InterruptedException e) {
						throw new Exception(
								"Cannot remove at the moment. Waiting for resource to come back.");

					}
				}
				allResource.remove(resource.getId());
				resources.remove(resource.getId());
				outgoingResource.remove(resource.getId());
				return true;
			}

			else {
				// We have the resource, delete it fast.
				resources.remove(resource);
				allResource.remove(resource.getId());
				return true;
			}
		} else {
			// We don't manage this resource.
			return false;
		}

	}

	public synchronized boolean removeNow(R resource) {

		// Do we manage the resource ??
		if (resource != null && allResource.contains(resource.getId())) {
			// Delete the resource, we don't care if it is out or not.
			resources.remove(resource);
			allResource.remove(resource.getId());
			outgoingResource.remove(resource.getId());
			return true;

		} else {
			// We don't manage this resource.
			return false;
		}

	}

	public synchronized R acquire() throws Exception {
		if (!isOpen || allResource.size() == 0)
			return null;
		else {
			R resource = resources.poll();
			while (resource == null) {
				try {
					wait();
					resource = resources.poll();
				} catch (InterruptedException e) {
					throw new Exception(
							"Waiting to acquire thread, hold ion !!");

				}
			}
			outgoingResource.add(resource.getId());
			return resource;
		}
	}

	public synchronized R acquire(long timeout,
			java.util.concurrent.TimeUnit unit) throws Exception {
		R resource = null;

		if (!isOpen || allResource.size() == 0)
			return null;
		else {
			resource = resources.poll();
			if (resource == null) {
				try {
					long startTime = System.currentTimeMillis();
					long waitTime = TimeUnit.MILLISECONDS
							.convert(timeout, unit);
					wait(waitTime);
					if (System.currentTimeMillis() <= (startTime + waitTime)) {
						// We got notified by a thread
						resource = resources.poll();
					}

					else {
						// we Timed out.
					}
				} catch (InterruptedException e) {
					throw new Exception(" I am wiating for the resource");
				}
			}
		}
		return resource;

	}

	public synchronized boolean release(R resource) {

		if (resource != null && outgoingResource.contains(resource.getId())) {
			resources.add(resource);
			outgoingResource.remove(resource.getId());
			notifyAll();
			return true;
		}

		else {
			return false;
		}
	}

	/**
	 * Clears all the resources from the pool for unit testing.
	 */
	public void clearResources() {
		allResource.clear();
		resources.clear();
		outgoingResource.clear();
	}
}
