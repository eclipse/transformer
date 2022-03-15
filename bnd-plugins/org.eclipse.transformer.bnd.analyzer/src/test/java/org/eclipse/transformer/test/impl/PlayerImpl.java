/********************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.test.impl;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManageableThread;

import org.eclipse.transformer.test.api.Ball;
import org.eclipse.transformer.test.api.Player;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class PlayerImpl implements Player {
	@Reference
	private Ball ball;
	@Reference
	private ManageableThread	thread;

	@Override
	public Ball getBall() {
		return ball;
	}

	@Override
	public void kickBall() {
		thread.isShutdown();
		try {
			ball.kick();
		} catch (AbortedException e) {
			e.printStackTrace();
		}
	}

}
