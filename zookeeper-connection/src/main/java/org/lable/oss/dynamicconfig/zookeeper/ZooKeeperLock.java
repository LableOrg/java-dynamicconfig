/*
 * Copyright (C) 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperLock {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperLock.class);

    static String ZNODE;
    static String QUEUE_NODE;
    final static String LOCKING_TICKET = "nr-00000000000000";

    static final Random random = new Random();

    final ZooKeeper zookeeper;

    protected State state = State.UNLOCKED;

    ZooKeeperLock(ZooKeeper zooKeeper, String znode) {
        ZNODE = znode;
        QUEUE_NODE = znode + "/queue";
        this.zookeeper = zooKeeper;
    }

    public void lock() throws IOException {
        if (state == State.LOCKED) return;

        try {
            acquireLock(zookeeper, QUEUE_NODE);
        } catch (KeeperException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        state = State.LOCKED;
    }

    public void unlock() throws IOException {
        if (state == State.UNLOCKED) return;

        try {
            releaseTicket(zookeeper, QUEUE_NODE, LOCKING_TICKET);
        } catch (KeeperException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        state = State.UNLOCKED;
    }

    /**
     * Try to acquire a lock on for choosing a resource. This method will wait until it has acquired the lock.
     *
     * @param zookeeper ZooKeeper connection to use.
     * @param lockNode  Path to the znode representing the locking queue.
     * @return Name of the first node in the queue.
     */
    static String acquireLock(ZooKeeper zookeeper, String lockNode) throws KeeperException, InterruptedException {
        // Inspired by the queueing algorithm suggested here:
        // http://zookeeper.apache.org/doc/current/recipes.html#sc_recipes_Queues

        // Acquire a place in the queue by creating an ephemeral, sequential znode.
        String placeInLine = takeQueueTicket(zookeeper, lockNode);
        logger.debug("Acquiring lock, waiting in queue: {}.", placeInLine);

        // Wait in the queue until our turn has come.
        return waitInLine(zookeeper, lockNode, placeInLine);
    }

    /**
     * Take a ticket for the queue. If the ticket was already claimed by another process,
     * this method retries until it succeeds.
     *
     * @param zookeeper ZooKeeper connection to use.
     * @param lockNode  Path to the znode representing the locking queue.
     * @return The claimed ticket.
     */
    static String takeQueueTicket(ZooKeeper zookeeper, String lockNode) throws InterruptedException, KeeperException {
        // The ticket number includes a random component to decrease the chances of collision. Collision is handled
        // neatly, but it saves a few actions if there is no need to retry ticket acquisition.
        String ticket = String.format("nr-%014d-%04d", System.currentTimeMillis(), random.nextInt(10000));
        if (grabTicket(zookeeper, lockNode, ticket)) {
            return ticket;
        } else {
            return takeQueueTicket(zookeeper, lockNode);
        }
    }

    /**
     * Release an acquired lock.
     *
     * @param zookeeper ZooKeeper connection to use.
     * @param lockNode  Path to the znode representing the locking queue.
     * @param ticket    Name of the first node in the queue.
     */
    static void releaseTicket(ZooKeeper zookeeper, String lockNode, String ticket)
            throws KeeperException, InterruptedException {

        logger.debug("Releasing ticket {}.", ticket);
        try {
            zookeeper.delete(lockNode + "/" + ticket, -1);
        } catch (KeeperException.NoNodeException e) {
            // If it the node is already gone, than that is fine.
        }
    }

    /**
     * Wait in the queue until the znode in front of us changes.
     *
     * @param zookeeper   ZooKeeper connection to use.
     * @param lockNode    Path to the znode representing the locking queue.
     * @param placeInLine Name of our current position in the queue.
     * @return Name of the first node in the queue, when we are it.
     */
    static String waitInLine(ZooKeeper zookeeper, String lockNode, String placeInLine)
            throws KeeperException, InterruptedException {

        // Get the list of nodes in the queue, and find out what our position is.
        List<String> children;
        try {
            children = zookeeper.getChildren(lockNode, false);
        } catch (KeeperException.NoNodeException e) {
            ZooKeeperHelper.mkdirp(zookeeper, lockNode);
            children = zookeeper.getChildren(lockNode, false);
        }

        // The list returned is unsorted.
        Collections.sort(children);

        if (children.size() == 0) {
            // Only possible if some other process cancelled our ticket.
            logger.warn("getChildren() returned empty list, but we created a ticket.");
            return acquireLock(zookeeper, lockNode);
        }

        boolean lockingTicketExists = children.get(0).equals(LOCKING_TICKET);
        if (lockingTicketExists) {
            children.remove(0);
        }

        // Where are we in the queue?
        int positionInQueue = -1;
        int i = 0;
        for (String child : children) {
            if (child.equals(placeInLine)) {
                positionInQueue = i;
                break;
            }
            i++;
        }

        if (positionInQueue < 0) {
            // Theoretically not possible.
            throw new RuntimeException("Created node (" + placeInLine + ") not found in getChildren().");
        }

        String placeBeforeUs;
        if (positionInQueue == 0) {
            // Lowest number in the queue, go for the lock.
            if (grabTicket(zookeeper, lockNode, LOCKING_TICKET)) {
                releaseTicket(zookeeper, lockNode, placeInLine);
                return LOCKING_TICKET;
            } else {
                placeBeforeUs = LOCKING_TICKET;
            }
        } else {
            // We are not in front of the queue, so we keep an eye on the znode right in front of us. When it is
            // deleted, that means it has reached the front of the queue, acquired the lock, did its business,
            // and released the lock.
            placeBeforeUs = children.get(positionInQueue - 1);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        Stat stat = zookeeper.exists(lockNode + "/" + placeBeforeUs, event -> {
            // If *anything* changes, reevaluate our position in the queue.
            latch.countDown();
        });

        // If stat is null, the znode in front of use got deleted during our inspection of the queue. If that happens,
        // simply reevaluate our position in the queue again. If there *is* a znode in front of us,
        // watch it for changes:
        if (stat != null) {
            logger.debug("Watching place in queue before us ({})", placeBeforeUs);
            latch.await();
        }

        return waitInLine(zookeeper, lockNode, placeInLine);
    }

    /**
     * Grab a ticket in the queue.
     *
     * @param zookeeper ZooKeeper connection to use.
     * @param lockNode  Path to the znode representing the locking queue.
     * @param ticket    Name of the ticket to attempt to grab.
     * @return True on success, false if the ticket was already grabbed by another process.
     */
    static boolean grabTicket(ZooKeeper zookeeper, String lockNode, String ticket)
            throws InterruptedException, KeeperException {
        try {
            zookeeper.create(lockNode + "/" + ticket, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException.NodeExistsException e) {
            // It is possible that two processes try to grab the exact same ticket at the same time.
            // This is common for the locking ticket.
            logger.debug("Failed to claim ticket {}.", ticket);
            return false;
        } catch (KeeperException.NoNodeException e) {
            // Parent node does not exist yet. Prepare it:
            ZooKeeperHelper.mkdirp(zookeeper, lockNode);
        }
        logger.debug("Claimed ticket {}.", ticket);
        return true;
    }

    /**
     * Internal state of this lock.
     */
    public enum State {
        LOCKED,
        UNLOCKED
    }
}
