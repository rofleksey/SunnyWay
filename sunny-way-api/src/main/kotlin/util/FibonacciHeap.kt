package util

import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.NoSuchElementException
import kotlin.require

class FibonacciHeap<T> {
    class Entry<T> constructor(elem: T, var priority: Double) {
        var mDegree = 0
        var mIsMarked = false
        var mNext: Entry<T> = this
        var mPrev: Entry<T> = this
        var mParent: Entry<T>? = null
        var mChild: Entry<T>? = null
        var value: T = elem
    }

    private var mMin: Entry<T>? = null
    private var mSize = 0

    fun enqueue(value: T, priority: Double): Entry<T> {
        val result = Entry(value, priority)
        mMin = mergeLists(mMin, result)
        ++mSize
        return result
    }

    fun min(): Entry<T> {
        if (mMin == null) {
            throw NoSuchElementException("Heap is empty.")
        }
        return mMin!!
    }

    fun size(): Int = mSize

    fun isEmpty(): Boolean = mSize == 0

    fun dequeueMin(): Entry<T> {
        if (mMin == null) {
            throw NoSuchElementException("Heap is empty.")
        }
        --mSize
        val minElem = mMin

        /* Now, we need to get rid of this element from the list of roots.  There
         * are two cases to consider.  First, if this is the only element in the
         * list of roots, we set the list of roots to be null by clearing mMin.
         * Otherwise, if it's not null, then we write the elements next to the
         * min element around the min element to remove it, then arbitrarily
         * reassign the min.
         */
        if (mMin!!.mNext == mMin) { // Case one
            mMin = null
        } else { // Case two
            mMin!!.mPrev.mNext = mMin!!.mNext
            mMin!!.mNext.mPrev = mMin!!.mPrev
            mMin = mMin!!.mNext // Arbitrary element of the root list.
        }

        /* Next, clear the parent fields of all of the min element's children,
         * since they're about to become roots.  Because the elements are
         * stored in a circular list, the traversal is a bit complex.
         */
        if (minElem!!.mChild != null) {
            /* Keep track of the first visited node. */
            var curr: Entry<T>? = minElem.mChild
            do {
                curr!!.mParent = null

                /* Walk to the next node, then stop if this is the node we
                 * started at.
                 */curr = curr.mNext
            } while (curr != minElem.mChild)
        }

        /* Next, splice the children of the root node into the topmost list,
         * then set mMin to point somewhere in that list.
         */
        mMin = mergeLists(mMin, minElem.mChild)

        /* If there are no entries left, we're done. */
        if (mMin == null) return minElem

        /* Next, we need to coalsce all of the roots so that there is only one
         * tree of each degree.  To track trees of each size, we allocate an
         * ArrayList where the entry at position i is either null or the
         * unique tree of degree i.
         */
        val treeTable: MutableList<Entry<T>?> = ArrayList()

        /* We need to traverse the entire list, but since we're going to be
         * messing around with it we have to be careful not to break our
         * traversal order mid-stream.  One major challenge is how to detect
         * whether we're visiting the same node twice.  To do this, we'll
         * spent a bit of overhead adding all of the nodes to a list, and
         * then will visit each element of this list in order.
         */
        val toVisit: MutableList<Entry<T>> = ArrayList()

        /* To add everything, we'll iterate across the elements until we
         * find the first element twice.  We check this by looping while the
         * list is empty or while the current element isn't the first element
         * of that list.
         */

        var tempCur: Entry<T> = mMin!!
        while (toVisit.isEmpty() || toVisit[0] != tempCur) {
            toVisit.add(tempCur)
            tempCur = tempCur.mNext
        }

        /* Traverse this list and perform the appropriate unioning steps. */
        for (curVisited in toVisit) {
            /* Keep merging until a match arises. */
            var cur = curVisited
            while (true) {
                /* Ensure that the list is long enough to hold an element of this
                 * degree.
                 */
                while (cur.mDegree >= treeTable.size) treeTable.add(null)

                /* If nothing's here, we're can record that this tree has this size
                 * and are done processing.
                 */
                if (treeTable[cur.mDegree] == null) {
                    treeTable[cur.mDegree] = cur
                    break
                }

                /* Otherwise, merge with what's there. */
                val other = treeTable[cur.mDegree]
                treeTable[cur.mDegree] = null // Clear the slot

                /* Determine which of the two trees has the smaller root, storing
                 * the two tree accordingly.
                 */
                val min = if (other!!.priority < cur.priority) other else cur
                val max = if (other.priority < cur.priority) cur else other

                /* Break max out of the root list, then merge it into min's child
                 * list.
                 */
                max.mNext.mPrev = max.mPrev
                max.mPrev.mNext = max.mNext

                /* Make it a singleton so that we can merge it. */
                max.mPrev = max
                max.mNext = max.mPrev
                min.mChild = mergeLists(min.mChild, max)

                /* Reparent max appropriately. */
                max.mParent = min

                /* Clear max's mark, since it can now lose another child. */
                max.mIsMarked = false

                /* Increase min's degree; it now has another child. */
                ++min.mDegree

                /* Continue merging this tree. */
                cur = min
            }

            /* Update the global min based on this node.  Note that we compare
             * for <= instead of < here.  That's because if we just did a
             * reparent operation that merged two different trees of equal
             * priority, we need to make sure that the min pointer points to
             * the root-level one.
             */
            if (cur.priority <= mMin!!.priority) {
                mMin = cur
            }
        }
        return minElem
    }

    /**
     * Decreases the key of the specified element to the new priority.  If the
     * new priority is greater than the old priority, this function throws an
     * IllegalArgumentException.  The new priority must be a finite double,
     * so you cannot set the priority to be NaN, or +/- infinity.  Doing
     * so also throws an IllegalArgumentException.
     *
     * It is assumed that the entry belongs in this heap.  For efficiency
     * reasons, this is not checked at runtime.
     *
     * @param entry The element whose priority should be decreased.
     * @param newPriority The new priority to associate with this entry.
     * @throws IllegalArgumentException If the new priority exceeds the old
     * priority, or if the argument is not a finite double.
     */
    fun decreaseKey(entry: Entry<T>, newPriority: Double) {
        require(newPriority <= entry.priority) { "New priority exceeds old." }
        decreaseKeyUnchecked(entry, newPriority)
    }

    /**
     * Deletes this Entry from the Fibonacci heap that contains it.
     *
     * It is assumed that the entry belongs in this heap.  For efficiency
     * reasons, this is not checked at runtime.
     *
     * @param entry The entry to delete.
     */
    fun delete(entry: Entry<T>) {
        decreaseKeyUnchecked(entry, Double.NEGATIVE_INFINITY)
        dequeueMin()
    }

    fun clear() {
        while (mMin != null) {
            delete(mMin!!)
        }
    }

    /**
     * Decreases the key of a node in the tree without doing any checking to ensure
     * that the new priority is valid.
     *
     * @param entry The node whose key should be decreased.
     * @param priority The node's new priority.
     */
    private fun decreaseKeyUnchecked(entry: Entry<T>, priority: Double) {
        entry.priority = priority
        /* If the node no longer has a higher priority than its parent, cut it.
         * Note that this also means that if we try to run a delete operation
         * that decreases the key to -infinity, it's guaranteed to cut the node
         * from its parent.
         */
        if (entry.mParent != null && entry.priority <= entry.mParent!!.priority) {
            cutNode(entry)
        }

        /* If our new value is the new min, mark it as such.  Note that if we
         * ended up decreasing the key in a way that ties the current minimum
         * priority, this will change the min accordingly.
         */
        if (entry.priority <= mMin!!.priority) {
            mMin = entry
        }
    }

    /**
     * Cuts a node from its parent.  If the parent was already marked, recursively
     * cuts that node from its parent as well.
     *
     * @param entry The node to cut from its parent.
     */
    private fun cutNode(entry: Entry<T>?) {
        /* Begin by clearing the node's mark, since we just cut it. */
        entry!!.mIsMarked = false

        /* Base case: If the node has no parent, we're done. */
        if (entry.mParent == null) {
            return
        }

        /* Rewire the node's siblings around it, if it has any siblings. */
        if (entry.mNext != entry) { // Has siblings
            entry.mNext.mPrev = entry.mPrev
            entry.mPrev.mNext = entry.mNext
        }

        /* If the node is the one identified by its parent as its child,
         * we need to rewrite that pointer to point to some arbitrary other
         * child.
         */
        if (entry.mParent!!.mChild == entry) {
            /* If there are any other children, pick one of them arbitrarily. */
            if (entry.mNext != entry) {
                entry.mParent!!.mChild = entry.mNext
            } else {
                entry.mParent!!.mChild = null
            }
        }

        /* Decrease the degree of the parent, since it just lost a child. */
        --entry.mParent!!.mDegree

        /* Splice this tree into the root list by converting it to a singleton
         * and invoking the merge subroutine.
         */
        entry.mNext = entry
        entry.mPrev = entry.mNext
        mMin = mergeLists(mMin, entry)

        /* Mark the parent and recursively cut it if it's already been
         * marked.
         */
        if (entry.mParent!!.mIsMarked) {
            cutNode(entry.mParent)
        } else {
            entry.mParent!!.mIsMarked = true
        }

        /* Clear the relocated node's parent; it's now a root. */
        entry.mParent = null
    }

    companion object {
        /**
         * Given two Fibonacci heaps, returns a new Fibonacci heap that contains
         * all of the elements of the two heaps.  Each of the input heaps is
         * destructively modified by having all its elements removed.  You can
         * continue to use those heaps, but be aware that they will be empty
         * after this call completes.
         *
         * @param one The first Fibonacci heap to merge.
         * @param two The second Fibonacci heap to merge.
         * @return A new util.FibonacciHeap containing all of the elements of both
         * heaps.
         */
        fun <T> merge(one: FibonacciHeap<T>, two: FibonacciHeap<T>): FibonacciHeap<T> {
            /* Create a new util.FibonacciHeap to hold the result. */
            val result = FibonacciHeap<T>()

            /* Merge the two Fibonacci heap root lists together.  This helper function
             * also computes the min of the two lists, so we can store the result in
             * the mMin field of the new heap.
             */
            result.mMin = mergeLists(one.mMin, two.mMin)

            /* The size of the new heap is the sum of the sizes of the input heaps. */
            result.mSize = one.mSize + two.mSize

            /* Clear the old heaps. */
            two.mSize = 0
            one.mSize = two.mSize
            one.mMin = null
            two.mMin = null

            /* Return the newly-merged heap. */
            return result
        }

        /**
         * Utility function which, given two pointers into disjoint circularly-
         * linked lists, merges the two lists together into one circularly-linked
         * list in O(1) time.  Because the lists may be empty, the return value
         * is the only pointer that's guaranteed to be to an element of the
         * resulting list.
         *
         * This function assumes that one and two are the minimum elements of the
         * lists they are in, and returns a pointer to whichever is smaller.  If
         * this condition does not hold, the return value is some arbitrary pointer
         * into the doubly-linked list.
         *
         * @param one A pointer into one of the two linked lists.
         * @param two A pointer into the other of the two linked lists.
         * @return A pointer to the smallest element of the resulting list.
         */
        private fun <T> mergeLists(one: Entry<T>?, two: Entry<T>?): Entry<T>? {
            /* There are four cases depending on whether the lists are null or not.
             * We consider each separately.
             */
            return if (one == null && two == null) { // Both null, resulting list is null.
                null
            } else if (one != null && two == null) { // Two is null, result is one.
                one
            } else if (one == null && two != null) { // One is null, result is two.
                two
            } else { // Both non-null; actually do the splice.
                /* This is actually not as easy as it seems.  The idea is that we'll
                      * have two lists that look like this:
                      *
                      * +----+     +----+     +----+
                      * |    |--N->|one |--N->|    |
                      * |    |<-P--|    |<-P--|    |
                      * +----+     +----+     +----+
                      *
                      *
                      * +----+     +----+     +----+
                      * |    |--N->|two |--N->|    |
                      * |    |<-P--|    |<-P--|    |
                      * +----+     +----+     +----+
                      *
                      * And we want to relink everything to get
                      *
                      * +----+     +----+     +----+---+
                      * |    |--N->|one |     |    |   |
                      * |    |<-P--|    |     |    |<+ |
                      * +----+     +----+<-\  +----+ | |
                      *                  \  P        | |
                      *                   N  \       N |
                      * +----+     +----+  \->+----+ | |
                      * |    |--N->|two |     |    | | |
                      * |    |<-P--|    |     |    | | P
                      * +----+     +----+     +----+ | |
                      *              ^ |             | |
                      *              | +-------------+ |
                      *              +-----------------+
                      *
                      */
                val oneNext = one!!.mNext // Cache this since we're about to overwrite it.
                one.mNext = two!!.mNext
                one.mNext.mPrev = one
                two.mNext = oneNext
                two.mNext.mPrev = two

                /* Return a pointer to whichever's smaller. */
                if (one.priority < two.priority) {
                    one
                } else {
                    two
                }
            }
        }
    }
}