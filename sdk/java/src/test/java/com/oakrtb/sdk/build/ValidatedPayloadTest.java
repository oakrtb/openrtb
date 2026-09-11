package com.oakrtb.sdk.build;

import com.oakrtb.sdk.schema.Issue;
import com.oakrtb.sdk.schema.Report;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedPayloadTest {

  @Test
  void okWhenResultIsOk() {
    byte[] json = "{\"id\":\"x\"}".getBytes();
    ValidatedPayload payload = new ValidatedPayload(json, Report.ok());
    assertTrue(payload.ok());
    assertSame(json, payload.json());
    assertTrue(payload.result().isOk());
  }

  @Test
  void notOkWhenResultHasErrors() {
    Report fail =
        Report.fail(new Issue("required", "/id", "missing"));
    ValidatedPayload payload = new ValidatedPayload(new byte[0], fail);
    assertFalse(payload.ok());
    assertFalse(payload.result().isOk());
    assertEquals(1, payload.result().getErrors().size());
  }

  @Test
  void notOkWhenResultIsNull() {
    ValidatedPayload payload = new ValidatedPayload(new byte[0], null);
    assertFalse(payload.ok());
  }
}
