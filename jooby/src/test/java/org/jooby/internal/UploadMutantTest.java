package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.jooby.Err;
import org.jooby.MockUnit;
import org.jooby.Upload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

@RunWith(PowerMockRunner.class)
public class UploadMutantTest {

  @Test(expected = Err.class)
  public void booleanValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).booleanValue();
        });
  }

  @Test(expected = Err.class)
  public void byteValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).byteValue();
        });
  }

  @Test(expected = Err.class)
  public void shortValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).shortValue();
        });
  }

  @Test(expected = Err.class)
  public void intValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).intValue();
        });
  }

  @Test(expected = Err.class)
  public void longValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).longValue();
        });
  }

  @Test(expected = Err.class)
  public void stringValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).stringValue();
        });
  }

  @Test(expected = Err.class)
  public void floatValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).floatValue();
        });
  }

  @Test(expected = Err.class)
  public void doubleValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).doubleValue();
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = Err.class)
  public void enumValue() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).enumValue(Enum.class);
        });
  }

  @Test(expected = Err.class)
  public void toListAny() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).toList(String.class);
        });
  }

  @Test
  public void toList() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          List<Upload> uploads = ImmutableList.of(unit.get(Upload.class));
          assertEquals(uploads, new UploadMutant("name", uploads).toList(Upload.class));
        });

    new MockUnit(Upload.class)
        .run(unit -> {
          List<Upload> uploads = ImmutableList.of(unit.get(Upload.class));
          assertEquals(uploads,
              new UploadMutant("name", uploads).to(TypeLiteral.get(Types.listOf(Upload.class))));
        });

    new MockUnit(Upload.class)
        .run(unit -> {
          List<Upload> uploads = ImmutableList.of(unit.get(Upload.class));
          assertEquals(uploads,
              new UploadMutant("name", uploads).to(new TypeLiteral<List<Upload>>() {
              }));
        });
  }

  @Test(expected = Err.class)
  public void toSetAny() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).toSet(String.class);
        });
  }

  @Test
  public void toSet() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          List<Upload> uploads = ImmutableList.of(unit.get(Upload.class));
          assertEquals(ImmutableSet.of(unit.get(Upload.class)),
              new UploadMutant("name", uploads).toSet(Upload.class));
        });

    new MockUnit(Upload.class)
        .run(unit -> {
          List<Upload> uploads = ImmutableList.of(unit.get(Upload.class));
          assertEquals(ImmutableSet.of(unit.get(Upload.class)),
              new UploadMutant("name", uploads).to(TypeLiteral.get(Types.setOf(Upload.class))));
        });

    new MockUnit(Upload.class)
        .run(unit -> {
          List<Upload> uploads = ImmutableList.of(unit.get(Upload.class));
          assertEquals(ImmutableSet.of(unit.get(Upload.class)),
              new UploadMutant("name", uploads).to(new TypeLiteral<Set<Upload>>() {
              }));
        });
  }

  @Test(expected = Err.class)
  public void toUnknown() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).to(Object.class);
        });
  }

  @Test(expected = Err.class)
  public void toSortedSetAny() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class)))
              .toSortedSet(String.class);
        });
  }

  @Test(expected = Err.class)
  public void toOptionalAny() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          new UploadMutant("name", ImmutableList.of(unit.get(Upload.class)))
              .toOptional(String.class);
        });
  }

  @Test
  public void isPresent() throws Exception {
    new MockUnit(Upload.class)
        .run(unit -> {
          assertEquals(true,
              new UploadMutant("name", ImmutableList.of(unit.get(Upload.class))).isPresent());
        });
  }

}
