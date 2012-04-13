/**
 * Copyright 2012 Alex Yanchenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.droidparts.inject;

import static org.droidparts.reflection.util.ReflectionUtils.listAnnotatedFields;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import org.droidparts.annotation.inject.InjectDependency;
import org.droidparts.annotation.inject.InjectBundleExtra;
import org.droidparts.annotation.inject.InjectResource;
import org.droidparts.annotation.inject.InjectSystemService;
import org.droidparts.annotation.inject.InjectView;
import org.droidparts.inject.injector.DependencyInjector;
import org.droidparts.inject.injector.BundleExtraInjector;
import org.droidparts.inject.injector.ResourceInjector;
import org.droidparts.inject.injector.SystemServiceInjector;
import org.droidparts.inject.injector.ViewInjector;
import org.droidparts.util.L;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

/**
 * <meta-data android:name="droidparts_dependency_provider"
 * android:value="com.yanchenko.android.sample.DependencyProvider" />
 */
public class Injector {

	protected static Context ctx;
	private static Injector injector;

	public static Injector get() {
		if (injector == null) {
			synchronized (Injector.class) {
				if (injector == null) {
					injector = new Injector();
				}
			}
		}
		return injector;
	}

	public void setUp(Context ctx) {
		Injector.ctx = ctx.getApplicationContext();
		DependencyInjector.init(Injector.ctx);
	}

	public void tearDown() {
		DependencyInjector.tearDown();
		ctx = null;
	}

	public void inject(Activity act) {
		// XXX
		View root = act.findViewById(android.R.id.content).getRootView();
		inject(act, root, act);
	}

	public void inject(Service serv) {
		inject(serv, null, serv);
	}

	public void inject(Context ctx, Object target) {
		inject(ctx, null, target);
	}

	public void inject(View view, Object target) {
		inject(view.getContext(), view, target);
	}

	public void inject(Object target) {
		if (ctx != null) {
			inject(ctx, null, target);
		} else {
			throw new IllegalStateException("No context provided.");
		}
	}

	protected void inject(Context ctx, View root, Object target) {
		long start = System.currentTimeMillis();
		if (Injector.ctx == null) {
			Injector.ctx = ctx.getApplicationContext();
		}
		final Class<?> cls = target.getClass();
		List<Field> fields = listAnnotatedFields(cls);
		for (Field field : fields) {
			for (Annotation ann : field.getAnnotations()) {
				Class<? extends Annotation> annType = ann.annotationType();
				boolean success = false;
				if (annType == InjectDependency.class) {
					success = DependencyInjector.inject(ctx, target, field);
				} else if (annType == InjectBundleExtra.class) {
					Bundle data = getIntentExtras(target);
					success = BundleExtraInjector.inject(ctx, data,
							(InjectBundleExtra) ann, target, field);
				} else if (annType == InjectResource.class) {
					success = ResourceInjector.inject(ctx,
							(InjectResource) ann, target, field);
				} else if (annType == InjectSystemService.class) {
					success = SystemServiceInjector.inject(ctx,
							(InjectSystemService) ann, target, field);
				} else if (annType == InjectView.class) {
					if (root != null) {
						success = ViewInjector.inject(ctx, root,
								(InjectView) ann, target, field);
					}
				} else {
					success = subInject(ctx, ann, target, field);
				}
				if (success) {
					break;
				}
			}
		}
		long end = System.currentTimeMillis() - start;
		L.d(String.format("Injected on %s in %d ms.", cls.getSimpleName(), end));
	}

	protected boolean subInject(Context ctx, Annotation ann, Object target,
			Field field) {
		return false;
	}

	protected Bundle getIntentExtras(Object obj) {
		Bundle data = null;
		if (obj instanceof Activity) {
			data = ((Activity) obj).getIntent().getExtras();
		} else if (obj instanceof Service) {
			// TODO
		}
		return data;
	}

}
