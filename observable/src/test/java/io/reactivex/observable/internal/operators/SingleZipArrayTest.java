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

package io.reactivex.observable.internal.operators;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.common.*;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.*;
import io.reactivex.observable.*;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;

public class SingleZipArrayTest {

    final BiFunction<Object, Object, Object> addString = new BiFunction<Object, Object, Object>() {
        @Override
        public Object apply(Object a, Object b) throws Exception {
            return "" + a + b;
        }
    };


    final Function3<Object, Object, Object, Object> addString3 = new Function3<Object, Object, Object, Object>() {
        @Override
        public Object apply(Object a, Object b, Object c) throws Exception {
            return "" + a + b + c;
        }
    };

    @Test
    public void firstError() {
        Single.zip(Single.error(new TestException()), Single.just(1), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void secondError() {
        Single.zip(Single.just(1), Single.<Integer>error(new TestException()), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Object> to = Single.zip(pp.single(0), pp.single(0), addString)
        .test();

        assertTrue(pp.hasObservers());

        to.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void zipperThrows() {
        Single.zip(Single.just(1), Single.just(2), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void zipperReturnsNull() {
        Single.zip(Single.just(1), Single.just(2), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void middleError() {
        PublishSubject<Integer> pp0 = PublishSubject.create();
        PublishSubject<Integer> pp1 = PublishSubject.create();

        TestObserver<Object> to = Single.zip(pp0.single(0), pp1.single(0), pp0.single(0), addString3)
        .test();

        pp1.onError(new TestException());

        assertFalse(pp0.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> pp0 = PublishSubject.create();
                final PublishSubject<Integer> pp1 = PublishSubject.create();

                final TestObserver<Object> to = Single.zip(pp0.single(0), pp1.single(0), addString)
                .test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp0.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayOneIsNull() {
        Single.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }, Single.just(1), null)
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyArray() {
        Single.zipArray(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(Object[] a) throws Exception {
                return a;
            }
        }, new SingleSource[0])
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void oneArray() {
        Single.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return (Integer)a[0] + 1;
            }
        }, Single.just(1))
        .test()
        .assertResult(2);
    }
}
