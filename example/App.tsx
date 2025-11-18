import {StyleSheet, Text, View, TouchableOpacity, StatusBar, Alert} from 'react-native';
import * as MediaLibrary from "expo-media-library";

import * as ExpoScreenRecorder from 'expo-screen-recorder';
import {useEffect, useState} from "react";

export default function App() {
    const [state, setState] = useState(ExpoScreenRecorder.getState());

  useEffect(() => {
    const interval = setInterval(() => {
      setState(ExpoScreenRecorder.getState())
    }, 500);
    return () => clearInterval(interval);
  }, []);


  const onPressStart = async () => {
    try {
      await ExpoScreenRecorder.startRecording();
        ExpoScreenRecorder.recordHDVideo(true).then(()=>{})
    } catch (err) {
      console.error("error", err)
    }
  }
  const onPressStop = async () => {
    try {
      const url = await ExpoScreenRecorder.stopRecording()
        const {status} = await MediaLibrary.getPermissionsAsync();
        if (status === "granted") {
            MediaLibrary.saveToLibraryAsync(url).then(() => {
               Alert.alert('saved to media library')
            }).catch((e) => {
                Alert.alert('error' + e)
            })
        } else {
            await MediaLibrary.requestPermissionsAsync();
        }
    } catch (err) {
      console.error("error", err)
    }
  }

  // const handlePauseResume = async () => {
  //   try{
  //       if (state === "paused") {
  //           await ExpoScreenRecorder.resumeRecording()
  //       } else {
  //           await ExpoScreenRecorder.pauseRecording()
  //       }
  //   } catch (err) {
  //       console.error("error", err)
  //   }
  // }
  return (
    <View style={styles.container}>
      <View style={styles.innerContainer}>
          <Text style={styles.stateText}>
              {state}
          </Text>
          {/*<View style={styles.otherButtons}>*/}
          {/*    <TouchableOpacity*/}
          {/*        style={{ paddingVertical: 8, paddingHorizontal: 16,  backgroundColor: state === "idle" ? "#00000085" : "black" , borderRadius: 50 }}*/}
          {/*        onPress={handlePauseResume}*/}
          {/*        // disabled={state === "idle"}*/}
          {/*    >*/}
          {/*        <Text style={{ color: "white", fontSize: 16 }}>{state === "paused" ? "Resume": "Pause"}</Text>*/}
          {/*    </TouchableOpacity>*/}
          {/*</View>*/}

          <View style={styles.buttonContainer}>
              <TouchableOpacity
                  style={{ margin: 20, padding: 20, backgroundColor: "black", borderRadius: 50 }}
                  onPress={onPressStart}
              >
                  <Text style={{ color: "white", fontSize: 16 }}>START RECORDING!</Text>
              </TouchableOpacity>
              <TouchableOpacity
                  style={{ margin: 20, padding: 20, backgroundColor: "black", borderRadius: 50 }}
                  onPress={onPressStop}
              >
                  <Text style={{ color: "white", fontSize: 16 }}>STOP RECORDING</Text>
              </TouchableOpacity>

          </View>
      </View>
        <StatusBar backgroundColor={'#00000050'} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
      paddingVertical: 36
  },
    innerContainer: {
      flex:1,
      width: "100%",
      alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: '15%',
        gap: 20
    },
    stateText: {
      fontSize: 20,
      fontWeight: "600",
      color: "black",
        textTransform: "capitalize"
    },
    otherButtons: {
      flexDirection: "row",
      gap: 20,
      alignItems: "center",
      justifyContent: "center"
    },
    buttonContainer: {
      flexGrow: 1,
        width: '100%',
        alignItems: 'center',
        justifyContent: 'flex-end',
    },
});
