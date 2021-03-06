/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.interop.internal.operators;

import org.reactivestreams.Subscription;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.*;
import io.reactivex.flowable.*;
import io.reactivex.flowable.extensions.FuseToFlowable;
import io.reactivex.flowable.internal.operators.FlowableElementAt;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observable.*;

public final class FlowableElementAtMaybe<T> extends Maybe<T> implements FuseToFlowable<T> {
    final Flowable<T> source;

    final long index;

    public FlowableElementAtMaybe(Flowable<T> source, long index) {
        this.source = source;
        this.index = index;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> s) {
        source.subscribe(new ElementAtSubscriber<T>(s, index));
    }

    @Override
    public Flowable<T> fuseToFlowable() {
        return RxJavaFlowablePlugins.onAssembly(new FlowableElementAt<T>(source, index, null, false));
    }

    static final class ElementAtSubscriber<T> implements RelaxedSubscriber<T>, Disposable {

        final MaybeObserver<? super T> actual;

        final long index;

        Subscription s;

        long count;

        boolean done;

        ElementAtSubscriber(MaybeObserver<? super T> actual, long index) {
            this.actual = actual;
            this.index = index;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long c = count;
            if (c == index) {
                done = true;
                s.cancel();
                s = SubscriptionHelper.CANCELLED;
                actual.onSuccess(t);
                return;
            }
            count = c + 1;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            s = SubscriptionHelper.CANCELLED;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            s = SubscriptionHelper.CANCELLED;
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            s.cancel();
            s = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return s == SubscriptionHelper.CANCELLED;
        }

    }
}
