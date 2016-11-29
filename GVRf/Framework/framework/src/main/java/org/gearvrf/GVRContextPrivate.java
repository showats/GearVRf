package org.gearvrf;

import org.gearvrf.GVRHybridObject.NativeCleanupHandler;
import org.gearvrf.utility.Log;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

final class GVRContextPrivate {
    /**
     * Our {@linkplain GVRReference references} are placed on this queue, once
     * they've been finalized
     */
    private final ReferenceQueue<GVRHybridObject> mReferenceQueue = new ReferenceQueue<GVRHybridObject>();
    /**
     * We need hard references to {@linkplain GVRReference our references} -
     * otherwise, the references get garbage collected (usually before their
     * objects) and never get enqueued.
     */
    private final Set<GVRReference> mReferenceSet = new HashSet<GVRReference>();

    int finalizeUnreachableObjects() {
        GVRReference reference;
        int count = 0;
        while (null != (reference = (GVRReference)mReferenceQueue.poll())) {
            reference.close();
            ++count;
            if (count == 20) {
//                return count;
            }
        }
        return count;
    }

    final class GVRReference extends PhantomReference<GVRHybridObject> {
        private long mNativePointer;
        private final Class clazz;
        private final List<NativeCleanupHandler> mCleanupHandlers;

        private GVRReference(GVRHybridObject object, long nativePointer,
                List<NativeCleanupHandler> cleanupHandlers) {
            super(object, mReferenceQueue);

            mNativePointer = nativePointer;
            mCleanupHandlers = cleanupHandlers;
            clazz = object.getClass();
        }

        private void close() {
            close(true);
        }

        private void close(boolean removeFromSet) {
            synchronized (mReferenceSet) {
                if (mNativePointer != 0) {
                    if (mCleanupHandlers != null) {
                        for (NativeCleanupHandler handler : mCleanupHandlers) {
                            handler.nativeCleanup(mNativePointer);
                        }
                    }
//                    if (GVRRenderData.class != clazz) {
//                    if (clazz == GVRMaterial.class || clazz == GVRMesh.class || clazz == GVRSceneObject.class
//                || clazz == GVRTransform.class || GVREyePointeeHolder.class == clazz /*|| GVRRenderData.class == clazz*/) {
//                        Log.i("mmarinov", "close " + clazz + ", " + Long.toHexString(mNativePointer));
                        NativeHybridObject.delete(mNativePointer);
//                    } else {
//                        Log.i("mmarinov", "not closing " + clazz);
//                    }
                    mNativePointer = 0;
                }

                if (removeFromSet) {
                    mReferenceSet.remove(this);
                }
            }
        }
    }

    void registerHybridObject(GVRHybridObject gvrHybridObject, long nativePointer, List<NativeCleanupHandler> cleanupHandlers) {
        synchronized (mReferenceSet) {
            mReferenceSet.add(new GVRReference(gvrHybridObject, nativePointer, cleanupHandlers));
        }
    }

    /**
     * Explicitly close()ing an object is going to be relatively rare - most
     * native memory will be freed when the owner-objects are garbage collected.
     * Doing a lookup in these rare cases means that we can avoid giving every @link
     * {@link GVRHybridObject} a hard reference to its {@link GVRReference}.
     */
    GVRReference findReference(long nativePointer) {
        for (GVRReference reference : mReferenceSet) {
            if (reference.mNativePointer == nativePointer) {
                return reference;
            }
        }
        return null;
    }

    final void releaseNative(GVRHybridObject hybridObject) {
        synchronized (mReferenceSet) {
            if (hybridObject.getNative() != 0L) {
                GVRReference reference = findReference(hybridObject.getNative());
                if (reference != null) {
                    reference.close();
                }
            }
        }
    }
}
