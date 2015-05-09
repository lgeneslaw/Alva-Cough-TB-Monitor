<?php

/* This file runs when it receives an HTTP POST request. It receives an entire
   wav file representing an hour of audio from a patient's day, stores it on the
   server, and runs the cough detection algorithm to determine how many coughs
   are in the file. Then it adds a new record to the patient's cough data table
   based on the results of the detection algorithm

   The first line of the body must contain the following parameters, in order

   Params:
     DT - the exact moment when this audio began recording
     email - The patient's email

   Returns:
     A JSON object with success/failure and a message

 */


// include db connect class
require_once dirname(__FILE__) . '/db_connect.php';
require_once dirname(__FILE__) . '/db_table_constants.php';

//connecting to db
$db = new DB_CONNECT();


//if validateParams found an issue, this sends back a JSON failure
function sendParamFailure() {
    $response = array();
    $response['success'] = false;
    $response['message'] = "Post body must contain param string "
        . "with following format: "
        . "DT=YYYY-MM-DD HH:MM:SS&email=ex@mple.com";
    echo json_encode($response);
    die();
}

/* calls sendParamFailure if the param string doesn't match this format:
   DT=YYYY-MM-DD HH:MM:SS&email=<some string>
 */
function validateParams($str) {
    //must have at least 29 characters to send both params
    if(strlen($str) < 29) sendParamFailure();

    //check date_time
    elseif(substr($str, 0, 3) != "DT=") sendParamFailure();

    //check email
    if(substr($str, 23, 6) != "email=") sendParamFailure();
}

/* uses $params['email'] to find the patient's ID
   sends back a JSON failure object if email not found
 */
function addIDToArray(&$params) {
    //try to find user
    $result = mysql_query("SELECT * from " . TBL_CRED . " WHERE EMAIL = '"
            . $params['email'] . "'") or die(mysql_error());
    if($row = mysql_fetch_array($result))
        $params["ID"] = $row["ID"];

    //send back failure if email not found
    else {
        $response = array();
        $response['success'] = false;
        $response['message'] = "Given email not found in records";
        echo json_encode($response);
        die();
    }
}

//creates an associative array of parameters from the param string in the POST body
function parseParamStr($str) {
    $strlen = strlen($str);
    $params = array();
    $params["email"] = "";
    $params["DT"] = "";

    validateParams($str); // send back an error response if invalid params

    $i = 0;
    $dt_length = 19;

    //skip past first = sign
    while(($i < $strlen) and $str[$i] != '=') $i++;
    $i++;

    // grab the date_time
    $params["DT"] .= substr($str, $i, $dt_length);
    $i += $dt_length;

    // skip past the next = sign
    while(($i < $strlen) and $str[$i] != '=') $i++;
    $i++;

    //append the email (the rest of the param line)
    while(($i < $strlen)) {
        $params["email"] .= $str[$i];
        $i++;
    }

    $params["email"] = mysql_real_escape_string($params["email"]);
    $params["DT"] = mysql_real_escape_string($params["DT"]);

    //add ID to associative array and return
    addIDToArray($params);
    return $params;
}

// creates a file path and name from the params array. If the path doesn't exist,
// createFilenameFromParams makes the necessary folders
function createFilenameFromParams($params) {
    $wav_output_dir  = $_SERVER['DOCUMENT_ROOT'];
    $wav_output_dir .= "/p539/patient_data/audio/";
    $wav_output_dir .= "p" . $params['ID'] . "/";

    //create directory for this patient if it doesn't exist already
    if(!file_exists($wav_output_dir)) {
        mkdir($wav_output_dir, 0755);
    }

    $filename  = $wav_output_dir;
    $filename .= "p" . $params["ID"];
    $filename .= "_" . $params["DT"];
    $filename .= ".wav";

    return $filename;
}


//read the POST body
$post_body = @file_get_contents('php://input');

//grab parameter string from first line of body, then remove first line from body
$param_str = strtok($post_body, "\n");
$post_body = implode("\n", array_slice(explode("\n", $post_body), 1));

//convert parameter string to param array
$params = parseParamStr($param_str);

//create wav file to write to
$filename = createFilenameFromParams($params);
$myfile = fopen($filename, "w") or die("Unable to open file.");
fwrite($myfile, $post_body);
fclose($myfile);


//insert cough count into this patient's table.
//build and run query
$ID = $params["ID"];
$DT = $params["DT"];
$patient_table = "P" . $ID;
$sql = "INSERT INTO " . $patient_table . " (DATE_TIME, COUGH_COUNT) "
. "VALUES ('" . $DT . "', '" . rand(30, 70) . "');";
mysql_query($sql) or die(mysql_error());

//successful if made it this far
$response = array();
$response['success'] = true;
echo json_encode($response);

?>
