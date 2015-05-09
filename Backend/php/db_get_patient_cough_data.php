<?php

/*
 * Following code will get cough data for one patient.
 
 Params:
   email (required) - patient email
   D1 (optional) - datetime in format 'YYYY-MM-DD HH:MM:SS'
   D2 (optional) - datetime in format 'YYYY-MM-DD HH:MM:SS'
     * if D1 and D2 present, they specify a range. If only D1
       is present, the entry at that specific datetime will be returned
   PT(optional) - return post-treatment data if set to 1, pre-treatment otherwise
 
   Returns:
     A JSON object with cough data and a success status
 */

// array for JSON response
$response = array();

// include db connect class
require_once dirname(__FILE__) . '/db_connect.php';
require_once dirname(__FILE__) . '/db_table_constants.php';

// connecting to db
$db = new DB_CONNECT();

//send back failure if no ID parameter passed
if (isset($_GET["email"])) {
    $email = mysql_real_escape_string($_GET['email']);
    //try to find user
    $result = mysql_query("SELECT * from " . TBL_CRED . " WHERE EMAIL = '"
                          . $email . "'") or die(mysql_error());
    if($row = mysql_fetch_array($result))
        $ID = $row["ID"];
    else
        die("invalid email");

    $table = "P" . $ID;

    //base command
    $base_sql = "SELECT * FROM " . $table;

    //if optional args passed, where_sql will be used to filter base_sql
    $where_sql = "";

    //if D1 passed but not D2, get all coughs from D1
    if(isset($_GET["D1"]) and !isset($_GET["D2"])) {
        $D1 = mysql_real_escape_string($_GET['D1']);
        $where_sql = " WHERE DATE_TIME = '" . $D1 . "'";
    }

    //if D1 and D2 passed, get all coughs between D1 and D2
    if(isset($_GET["D1"]) and isset($_GET["D2"])) {
        $D1 = mysql_real_escape_string($_GET['D1']);
        $D2 = mysql_real_escape_string($_GET['D2']);
        $where_sql = " WHERE DATE_TIME BETWEEN '" . $D1 . "' AND '" . $D2 . "'";
    }
    
    //if PT passed, filter based on pre/post treatment
    if(isset($_GET["PT"])) {
        $PT = mysql_real_escape_string($_GET['PT']);
        if(empty($where_sql))
            $where_sql = " WHERE POST_TREATMENT = " . $PT;
        else
            $where_sql .= " AND POST_TREATMENT = " . $PT;
    }

    //build and run query
    $sql = $base_sql . $where_sql . ";";
    $result = mysql_query($sql) or die(mysql_error());

    // check for empty result
    if (mysql_num_rows($result) > 0) {
        // looping through all results
        // cough counts node
        $response["cough counts"] = array();

        while ($row = mysql_fetch_array($result)) {
            // temp user array
            $cough_counts = array();
            $cough_counts["date time"]      = $row["DATE_TIME"];
            $cough_counts["cough count"]    = $row["COUGH_COUNT"];
            $cough_counts["post treatment"] = $row["POST_TREATMENT"];

            // push single cough count into final response array
            array_push($response["cough counts"], $cough_counts);
        }
        // success
        $response["success"] = true;

        // echoing JSON response
        echo json_encode($response);
    } else {
        // no cough entries found
        $response["success"] = false;
        if(isset($_GET["D1"]))
            $response["message"] = "No cough entries found for specified date(s) or time(s)";
        else
            $response["message"] = "No cough entries found";


        // echo no users JSON
        echo json_encode($response);
    }

} else {
    // required field is missing
    $response["success"] = false;
    $response["message"] = "Required field(s) missing";

    // echoing JSON response
    echo json_encode($response);
}
?>

