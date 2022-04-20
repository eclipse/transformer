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

package org.eclipse.transformer.util;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.transformer.util.SignatureUtils.keyStream;
import static org.eclipse.transformer.util.SignatureUtils.packageMatch;
import static org.eclipse.transformer.util.SignatureUtils.segments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

import org.eclipse.transformer.util.SignatureUtils.KeySpliterator;
import org.eclipse.transformer.util.SignatureUtils.RenameKeyComparator;
import org.junit.jupiter.api.Test;

import aQute.bnd.unmodifiable.Sets;

/**
 *
 */
class SignatureUtilsTest {
	@Test
	void package_match_stems() {
		String text = "a.b.c.f.Foo";
		int matchEnd = packageMatch(text, 0, 3, true);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b.c.f");
		text = "a.b.c.f.Foo";
		matchEnd = packageMatch(text, 0, 7, true);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b.c.f");
		text = "a.b.c.f";
		matchEnd = packageMatch(text, 0, 3, true);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b.c.f");
		text = "a.b.c.";
		matchEnd = packageMatch(text, 0, 3, true);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b.c");
		text = "a.b";
		matchEnd = packageMatch(text, 0, 3, true);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b");
		text = "a";
		matchEnd = packageMatch(text, 0, 1, true);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a");

		text = "a.bb";
		matchEnd = packageMatch(text, 0, 3, true);
		assertThat(matchEnd).isEqualTo(-1);
		text = "aa.b";
		matchEnd = packageMatch(text, 1, 4, true);
		assertThat(matchEnd).isEqualTo(-1);
	}

	@Test
	void package_match() {
		String text = "a.b.c.f.Foo";
		int matchEnd = packageMatch(text, 0, 3, false);
		assertThat(matchEnd).isEqualTo(-1);
		text = "a.b.c.f.Foo";
		matchEnd = packageMatch(text, 0, 7, false);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b.c.f");
		text = "a.b.c.f";
		matchEnd = packageMatch(text, 0, 3, false);
		assertThat(matchEnd).isEqualTo(-1);
		text = "a.b.c.";
		matchEnd = packageMatch(text, 0, 3, false);
		assertThat(matchEnd).isEqualTo(-1);
		text = "a.b";
		matchEnd = packageMatch(text, 0, 3, false);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a.b");
		text = "a";
		matchEnd = packageMatch(text, 0, 1, false);
		assertThat(text.substring(0, matchEnd)).isEqualTo("a");

		text = "a.bb";
		matchEnd = packageMatch(text, 0, 3, false);
		assertThat(matchEnd).isEqualTo(-1);
		text = "aa.b";
		matchEnd = packageMatch(text, 1, 4, false);
		assertThat(matchEnd).isEqualTo(-1);
	}

	@Test
	void rename_key_segments() {
		assertThat(segments("a", '.')).isEqualTo(1);
		assertThat(segments("a.b", '.')).isEqualTo(2);
		assertThat(segments("a.b.c", '.')).isEqualTo(3);
		assertThat(segments("a.b.c.d", '.')).isEqualTo(4);
		assertThat(segments("a.b.c.*", '.')).isEqualTo(4);
	}

	@Test
	void rename_key_compare_segments() {
		RenameKeyComparator comparator = new RenameKeyComparator('.');
		assertThat("a").usingComparator(comparator)
			.isGreaterThan("a.b");
		assertThat("a.b.c").usingComparator(comparator)
			.isLessThan("a.b");
		assertThat("a.b.c").usingComparator(comparator)
			.isEqualTo("a.b.c");
	}

	@Test
	void rename_key_compare_wildcard() {
		RenameKeyComparator comparator = new RenameKeyComparator('.');
		assertThat("a.*").usingComparator(comparator)
			.isGreaterThan("a.b");
		assertThat("a.b.c").usingComparator(comparator)
			.isLessThan("a.*");
		assertThat("a.b.*").usingComparator(comparator)
			.isEqualTo("a.b.*");
	}

	@Test
	void rename_key_compare_sorting() {
		RenameKeyComparator comparator = new RenameKeyComparator('.');
		Set<String> input = Sets.of("a.b.*", "b.c", "a", "a.*", "a.b", "a.b.c");
		List<String> sorted = input.stream()
			.sorted(comparator)
			.collect(toList());
		assertThat(sorted).containsExactly("a.b.c", "a.b", "b.c", "a", "a.b.*", "a.*");
	}

	@Test
	void key_spliterator_tryadvance() {
		Spliterator<String> spliterator = new KeySpliterator("a.b.c.d", ".*");
		List<String> list = new ArrayList<>();
		while (spliterator.tryAdvance(list::add))
			;
		assertThat(list).containsExactly("a.b.c.d", "a.b.c.d.*", "a.b.c.*", "a.b.*", "a.*");
	}

	@Test
	void key_spliterator_foreachremaining() {
		Spliterator<String> spliterator = new KeySpliterator("a/b/c/d", "/*");
		List<String> list = new ArrayList<>();
		spliterator.forEachRemaining(list::add);
		assertThat(list).containsExactly("a/b/c/d", "a/b/c/d/*", "a/b/c/*", "a/b/*", "a/*");
	}

	@Test
	void key_stream() {
		Stream<String> stream = keyStream("a.b.c.d", ".*");
		assertThat(stream).containsExactly("a.b.c.d", "a.b.c.d.*", "a.b.c.*", "a.b.*", "a.*");
		stream = keyStream("a", ".*");
		assertThat(stream).containsExactly("a", "a.*");
		stream = keyStream("/a/b/c", "/*");
		assertThat(stream).containsExactly("/a/b/c", "/a/b/c/*", "/a/b/*", "/a/*");
	}
}
