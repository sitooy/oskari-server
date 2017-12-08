package fi.nls.test.control;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

public class MockServletOutputStream extends ServletOutputStream {

    private final OutputStream out;

    public MockServletOutputStream(final OutputStream out) {
        this.out = out;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // Ignore
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

}
