// LogFragment.java
package com.deadmole.copytothebox.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.deadmole.copytothebox.R;
import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.PathUtils;
import com.deadmole.copytothebox.util.Constants;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;

public class LogFragment extends Fragment {

    private TextView textLogOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_logs, container, false);

        textLogOutput = rootView.findViewById(R.id.text_log_output); // elem to write to
        Button btnFetchLog = rootView.findViewById(R.id.button_view_log);  // button id

        btnFetchLog.setOnClickListener(v -> fetchAndDisplayLog());

        return rootView;
    }

    private void fetchAndDisplayLog() {
        String logPath = PathUtils.getLogPath(requireContext());
        int linesToRead = Constants.LOG_LINES_TO_READ;

        new Thread(() -> {
            String lastLines = readLastNLines(logPath, linesToRead);

            // Update UI on main thread
            requireActivity().runOnUiThread(() -> textLogOutput.setText(lastLines));
        }).start();
    }

    /**
     * Reads the last n lines from a file at given path.
     */
    private String readLastNLines(String filePath, int n) {
        File logFile = new File(filePath);
        if (!logFile.exists() || !logFile.canRead()) {
            Logger.log( getContext(), "Log fragment reports: Log file not found");
            return "nowt found";
        }

        final int BUFFER_SIZE = 8192;
        try (RandomAccessFile file = new RandomAccessFile(logFile, "r")) {
            long fileLength = file.length();
            long pointer = fileLength - 1;
            int lineCount = 0;
            StringBuilder sb = new StringBuilder();

            byte[] buffer = new byte[BUFFER_SIZE];
            int readLength;
            long seekPosition;

            while (pointer >= 0 && lineCount <= n) {
                seekPosition = Math.max(pointer - BUFFER_SIZE + 1, 0);
                readLength = (int) (pointer - seekPosition + 1);
                file.seek(seekPosition);
                file.readFully(buffer, 0, readLength);

                for (int i = readLength - 1; i >= 0; i--) {
                    if (buffer[i] == '\n') {
                        lineCount++;
                        if (lineCount > n) {
                            // Extract from next char after this newline to end of buffer
                            int start = i + 1;
                            sb.insert(0, new String(buffer, start, readLength - start));
                            return sb.toString();
                        }
                    }
                }
                sb.insert(0, new String(buffer, 0, readLength));
                pointer = seekPosition - 1;
            }

            return sb.toString();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();
            Logger.log( requireContext(), "unable to read log file " + stackTrace );
            return "failed";
        }
    }
}


