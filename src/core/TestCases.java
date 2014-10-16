package core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases.
 * 
 * @author manoj
 * 
 */

public class TestCases {

	@Before
	public void setup() {
		Pool<R> resourcePool = Pool.getPoolInstance();

	}

	@After
	public void clear() {
		Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.clearResources();
	}

	/*
	 * The pool shall not allow any resources to be acquired unless the pool is
	 * open.
	 */
	@Test
	public void acquireResourcesWithPoolClosedShouldBeFalse() {
		Resource r1 = new Resource();
		Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		try {
			resourcePool.closeNow();
			Assert.assertEquals(null, resourcePool.acquire());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Release resources anytime.
	 */
	@Test
	public void releaseResourceAnytime() {
		Resource r1 = new Resource();
		Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		Assert.assertEquals(new Integer(1), resourcePool.getSize());
	}

	/*
	 * Acquire method must block until a resource is avilable, if pool closed
	 * return null. If the pool is closed, then acquire gives null If the pool
	 * is open and the resource is out, then thread waits till it gets the
	 * resource
	 */

	@Test
	public void acquireMethodMustBlockUnlessResourceIsAvaiableOtherwiseReturnNull()
			throws InterruptedException {
		Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		resourcePool.open();
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				Resource r1 = null;
				try {
					r1 = (Resource) resourcePool.acquire();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// Let me close the pool first and then release
				resourcePool.closeNow();
				try {
					Thread.currentThread().sleep(2000);
					System.out
							.println("Thread thread1: i got the resource and I am sleeping");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out
						.println("Thread thread1: I am releasing the resource");
				resourcePool.release(r1);
			}
		};
		thread1.start();
		System.out.println("Thread currentThread : I am sleeping");
		Thread.currentThread().sleep(2000);
		System.out.println("Thread currentThread : I want a resource !!!");
		try {
			Assert.assertEquals("Since the pool closed no resource", null,
					resourcePool.acquire());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out
				.println("Thread currentThread: Resource Pool closed , so no resource");
		System.out
				.println("Thread currentThread : Let me open the resourcepool");
		resourcePool.open();
		Resource resource = null;
		try {
			resource = (Resource) resourcePool.acquire();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertEquals("Pool opened, so I should get the resource",
				resource, r1);

	}

	/*
	 * Add method returns true for unique resource and false for duplicate
	 * resource.
	 */
	@Test
	public void addMethodReturnsTrueForUniqueResourceAndFalseForDuplicateResource()
			throws InterruptedException {
		final Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();

		Thread thread1 = new Thread() {
			@Override
			public void run() {
				Assert.assertTrue("Adding a resource First time returns true",
						resourcePool.add(r1));
			}
		};
		thread1.start();
		Thread.currentThread().sleep(100);
		Assert.assertFalse("Adding a resource twice returns false",
				resourcePool.add(r1));
	}

	/*
	 * Removing the resource the first time (if the resource exists) is true,
	 * next time is false.
	 */
	@Test
	public void removeResourceFirstTimeIsTrueNextTimeIsFalse() {
		final Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					Assert.assertTrue(
							"Removing the resource the first time is true",
							resourcePool.remove(r1));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread1.start();
		try {
			Thread.currentThread().sleep(200);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			Assert.assertFalse(
					"Removing the resource the second time is false",
					resourcePool.remove(r1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * If a given resource is in use, then calling remove should make the thread
	 * wait, till it acquires it from pool.
	 */
	@Test
	public void waitTillTheResourceIsBackInthePoolToRemove() {
		Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					Resource resource = (Resource) resourcePool.acquire();
					System.out
							.println(" Thread1 : I acquired the resource and I am sleeping");
					Thread.currentThread().sleep(2000);
					resourcePool.release(resource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread1.start();
		boolean removeOperation = false;
		try {
			removeOperation = resourcePool.remove(r1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Assert.assertTrue(
				"Current Thread: Successfully removed resource after waiting",
				removeOperation);

	}

	/*
	 * Wait till I get back all the resources and the Close !!
	 */
	@Test
	public void closePoolButWaitTillIgetBackAllTheResources() {
		Resource r1 = new Resource();
		Resource r2 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		final Pool<R> resourcePool2 = Pool.getPoolInstance();
		resourcePool2.add(r2);
		Assert.assertEquals("Resource Pool is Singleton", new Integer(2),
				resourcePool.getSize());
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					Resource resource = (Resource) resourcePool2.acquire();
					Thread.currentThread().sleep(200);
					resourcePool2.release(resource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Thread thread2 = new Thread() {
			@Override
			public void run() {
				try {
					Resource resource = (Resource) resourcePool.acquire();
					Thread.currentThread().sleep(200);
					resourcePool.release(resource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread1.start();
		thread2.start();
		resourcePool.close();
		Assert.assertTrue(
				"If this statrement is reached then we got back all the resource",
				true);

	}

	/*
	 * Close now, no wait !!!
	 */
	@Test
	public void closeNow() {
		Resource r1 = new Resource();
		Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		try {
			resourcePool.closeNow();
			Assert.assertEquals(null, resourcePool.acquire());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Acquire with timeout .. resource never comes back in given time.
	 */
	@Test
	public void acquireWithTimeOut() {
		Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.open();
		resourcePool.add(r1);
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					Resource resource = (Resource) resourcePool.acquire();
					Thread.currentThread().sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		try {
			thread1.start();
			Thread.currentThread().sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Resource resource = null;
		try {
			resource = (Resource) resourcePool.acquire(10000,
					TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertEquals(
				"Resource doesn't come back within the duration of timeout time",
				null, resource);
	}

	/*
	 * Acquire with timeout .. resource comes back and get up.
	 */
	@Test
	public void acquireWithTimeOutResourceGetsBack() {
		Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.open();
		resourcePool.add(r1);
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					Resource resource = (Resource) resourcePool.acquire();
					// Release after 600 seconds
					Thread.currentThread().sleep(600);
					resourcePool.release(resource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		try {
			thread1.start();
			Thread.currentThread().sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Resource resource = null;
		try {
			resource = (Resource) resourcePool.acquire(10000,
					TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertEquals(
				" Resource comes back after certian time and we get notified within our timeout time",
				r1, resource);
	}

	/*
	 * Only release if not deleted.
	 */
	@Test
	public void releaseResourceBackToThePoolIfNotDeleted() {
		Resource r1 = new Resource();
		final Pool<R> resourcePool = Pool.getPoolInstance();
		resourcePool.add(r1);
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					Resource resource = (Resource) resourcePool.acquire();
					System.out
							.println(" Thread1 : I acquired the resource and I am removing it");
					Thread.currentThread().sleep(2000);
					resourcePool.remove(resource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		thread1.start();
		try {
			Thread.currentThread().sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Assert.assertFalse("The resource has been already deleted",
				resourcePool.release(r1));
	}

	/**
	 * Radom threads doing some work
	 */


	public void randomTest() {
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		final Pool<R> resourcePool = Pool.getPoolInstance();
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				Resource resource1 = new Resource();

				resourcePool.open();

				// Add three resources
				resourcePool.add(resource1);

				try {
					Resource resource = (Resource) resourcePool.acquire();

					Thread.currentThread().sleep(5000);
					resourcePool.release(resource);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
		Resource resource = null;

		try {
			resource = (Resource) resourcePool.acquire(5000,
					TimeUnit.MILLISECONDS);
			Assert.assertEquals("Nothing in the pool", null, resource);
			resource = (Resource) resourcePool.acquire(3500,
					TimeUnit.MILLISECONDS);
			Assert.assertNotNull("I should get a resource now", resource);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
