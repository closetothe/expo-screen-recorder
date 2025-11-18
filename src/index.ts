import { Platform } from "react-native";
import ExpoScreenRecorderModule from "./ExpoScreenRecorderModule";

/**
 * Start recording the screen
 * @param micEnabled - Enable microphone audio recording (default: false)
 */
export async function startRecording(
    micEnabled: boolean = false
): Promise<void> {
    return ExpoScreenRecorderModule.startRecording(micEnabled);
}

/**
 * Stop recording the screen
 * @param fileName - The name of the output file (don't include the file extension, we append ".mp4" to the fileName you provide). Default is a UUID.
 * @returns The file path of the recorded video
 */
export async function stopRecording(fileName?: string): Promise<string> {
    return ExpoScreenRecorderModule.stopRecording(fileName);
}

// TODO:
//  The ExpoModule.kt file has implementation for pausing the recording but there is a logical bug which causes the pause functionality to do nothing.
//  Or it could be unsupported in the first place, yet HBRecorder has these functions available;
// /**
//  * Pause the current recording (Android only)
//  * Must be called while recording is in progress
//  * @throws Error on iOS or if recording is not in progress
//  */
// export async function pauseRecording(): Promise<void> {
//     if (Platform.OS !== "android") {
//         throw new Error("pauseRecording is only available on Android");
//     }
//     return ExpoScreenRecorderModule.pauseRecording();
// }

// TODO: this function is implemented but there is a logical bug with the pause functionality,
//  therefore cannot say for sure if this is working too.
// /**
//  * Resume a paused recording (Android only)
//  * Must be called while recording is paused
//  * @throws Error on iOS or if recording is not paused
//  */
// export async function resumeRecording(): Promise<void> {
//     if (Platform.OS !== "android") {
//         throw new Error("resumeRecording is only available on Android");
//     }
//     return ExpoScreenRecorderModule.resumeRecording();
// }

/**
 * Check if the recorder is currently busy recording (Android only)
 * @returns true if recording is in progress, false otherwise
 * @throws Error on iOS
 */
export function isBusyRecording(): boolean {
    if (Platform.OS !== "android") {
        throw new Error("isBusyRecording is only available on Android");
    }
    return ExpoScreenRecorderModule.isBusyRecording();
}

/**
 * Get the current state of the recorder (Android only)
 * @returns The current state of the recorder
 * @throws Error on iOS
 */

type State = "idle" | "recording" | "paused";

export function getState(): State {
    if (Platform.OS !== "android") {
        throw new Error("getState is only available on Android");
    }
    return ExpoScreenRecorderModule.getState();
}

/**
 * Enable or disable HD video recording (Android only)
 * Must be called BEFORE startRecording() to take effect
 * @param enable - true to enable HD video, false to disable
 * @throws Error on iOS or if recording is already in progress
 */
export async function recordHDVideo(enable: boolean): Promise<void> {
    if (Platform.OS !== "android") {
        throw new Error("recordHDVideo is only available on Android");
    }
    return ExpoScreenRecorderModule.recordHDVideo(enable);
}