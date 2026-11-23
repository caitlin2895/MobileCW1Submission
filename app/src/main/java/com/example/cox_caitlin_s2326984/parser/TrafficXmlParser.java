package com.example.cox_caitlin_s2326984.parser;

import android.util.Log;

import com.example.cox_caitlin_s2326984.model.GeoPoint;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.model.SituationLocation;
import com.example.cox_caitlin_s2326984.model.SituationRecord;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

// Parses a DATEX II feed with XmlPullParser (Lab 3 PullParser example) into a list of
// Situation objects. The same parser is used for both the incidents and the roadworks feeds
public class TrafficXmlParser {
    private ArrayList<Situation> allSituations;
    private String publicationTime; // when Traffic Scotland generated the feed

    public String getPublicationTime() {
        return publicationTime;
    }

    // feedType is Situation.INCIDENT or Situation.ROADWORKS and is stored in every Situation.
    // Returns an empty list (never null) if there is nothing to parse
    public ArrayList<Situation> parseData(String dataToParse, String feedType) {
        allSituations = new ArrayList<>();
        publicationTime = null;

        // Clean up any leading garbage characters (as in the skeleton code),
        // but don't crash if the download failed and there is no XML
        if (dataToParse == null || dataToParse.indexOf("<") < 0) {
            Log.e("MyTag", "No XML data to parse for " + feedType);
            return allSituations;
        }
        int start = dataToParse.indexOf("<?");
        if (start < 0) {
            start = dataToParse.indexOf("<");
        }
        dataToParse = dataToParse.substring(start);

        Situation aSituation = null;      // like aThing in Lab 3
        SituationRecord aRecord = null;   // a situation holds one or more records
        GeoPoint aPoint = null;           // one set of lat/long inside groupOfLocations
        String pointTagName = null;       // which tag started that point (from, to, point...)
        String descriptorValue = null;    // place name waiting for its <tpegDescriptorType>

        try {
            // flags indicating which block the parser is traversing (like insideAThing).
            // Only needed where a tag name is used in more than one place, e.g. <value>
            boolean insideASituation = false;
            boolean insideARecord = false;
            boolean insideComment = false;
            boolean insideLocations = false;
            boolean insideAPoint = false;

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput( new StringReader( dataToParse ) );
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) { // Found a start tag - check which one, as we'd do different things
                    String tagName = xpp.getName();

                    if (tagName.equalsIgnoreCase("publicationTime") && !insideASituation) {
                        publicationTime = xpp.nextText();
                        Log.d("MyTag","Feed published at " + publicationTime);
                    } else if (tagName.equalsIgnoreCase("situation")) {
                        insideASituation = true;
                        //Start a new Situation object
                        aSituation = new Situation(feedType);
                        // <situation id="..." version="..."> - check each attribute by name
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equals("id")) {
                                aSituation.setId(xpp.getAttributeValue(i));
                            } else if (xpp.getAttributeName(i).equals("version")) {
                                aSituation.setVersion(xpp.getAttributeValue(i));
                            }
                        }
                        Log.d("MyTag","New situation found! id=" + aSituation.getId());
                    } else if (tagName.equalsIgnoreCase("overallSeverity")) {
                        String temp = xpp.nextText();
                        if(insideASituation){
                            aSituation.setOverallSeverity(temp);
                            Log.d("MyTag","Severity is " + temp);
                        }
                    } else if (tagName.equalsIgnoreCase("situationRecord")) {
                        if(insideASituation){
                            insideARecord = true;
                            //Start a new SituationRecord object
                            aRecord = new SituationRecord();
                            // <situationRecord xsi:type="Accident" id="..." version="...">
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                String value = xpp.getAttributeValue(i);
                                if (xpp.getAttributeName(i).equals("type")) {
                                    // keep only the part after any prefix, e.g. "d2:Accident" -> "Accident"
                                    aRecord.setRecordType(value.substring(value.indexOf(":") + 1));
                                } else if (xpp.getAttributeName(i).equals("id")) {
                                    aRecord.setId(value);
                                } else if (xpp.getAttributeName(i).equals("version")) {
                                    aRecord.setVersion(value);
                                }
                            }
                            Log.d("MyTag","New situation record found, type: " + aRecord.getRecordType());
                        }
                    } else if (tagName.equalsIgnoreCase("probabilityOfOccurrence")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.setProbability(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("severity")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.setSeverity(temp);
                            Log.d("MyTag","Record severity is " + temp);
                        }
                    }

                    // ---------------- <validity> tags ----------------
                    else if (tagName.equalsIgnoreCase("validityStatus")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getValidity().setValidityStatus(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("overallStartTime")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            // setStartTime() also breaks the date text down (like setBolt() in Thing)
                            aRecord.getValidity().setStartTime(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("overallEndTime")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getValidity().setEndTime(temp);
                        }
                    }

                    // ---------------- <impact> tags ----------------
                    else if (tagName.equalsIgnoreCase("delayTimeValue")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getImpact().setDelaySeconds(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("numberOfLanesRestricted")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getImpact().setLanesRestricted(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("numberOfOperationalLanes")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getImpact().setOperationalLanes(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("capacityRemaining")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getImpact().setCapacityRemaining(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("trafficConstrictionType")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.getImpact().setTrafficConstrictionType(temp);
                        }
                    }

                    // ---------------- <generalPublicComment> block ----------------
                    else if (tagName.equalsIgnoreCase("generalPublicComment")) {
                        insideComment = true;
                    } else if (tagName.equalsIgnoreCase("value")) {
                        // <value> is used in several places, so the flags decide what it is
                        // (same idea as the two kinds of <description> in Lab 3)
                        String temp = xpp.nextText();
                        if(insideARecord && insideComment){
                            aRecord.addToComment(temp);
                        } else if(insideARecord && insideAPoint){
                            descriptorValue = temp;
                        }
                        // any other <value> (e.g. a description of an area) is ignored
                    }

                    // ---------------- <groupOfLocations> block ----------------
                    else if (tagName.equalsIgnoreCase("groupOfLocations")) {
                        if(insideARecord){
                            insideLocations = true;
                            // xsi:type is Linear or Point
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals("type")) {
                                    String value = xpp.getAttributeValue(i);
                                    aRecord.getLocation().setLocationType(value.substring(value.indexOf(":") + 1));
                                }
                            }
                        }
                    } else if (insideLocations && !insideAPoint
                            && (tagName.equals("from") || tagName.equals("to")
                            || tagName.equals("point") || tagName.equals("locationForDisplay")
                            || tagName.equals("linearCoordinatesStartPoint")
                            || tagName.equals("linearCoordinatesEndPoint"))) {
                        // a tag holding one set of coordinates: start a new GeoPoint
                        insideAPoint = true;
                        aPoint = new GeoPoint();
                        pointTagName = tagName;
                    } else if (tagName.equalsIgnoreCase("latitude")) {
                        String temp = xpp.nextText();
                        if(insideAPoint){
                            aPoint.setLatitude(temp);
                        } else if(insideLocations){
                            // lat/long outside a known point tag (e.g. <pointByCoordinates>):
                            // keep it as the location's display point
                            if(aRecord.getLocation().getDisplayPoint() == null){
                                aRecord.getLocation().setDisplayPoint(new GeoPoint());
                            }
                            aRecord.getLocation().getDisplayPoint().setLatitude(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("longitude")) {
                        String temp = xpp.nextText();
                        if(insideAPoint){
                            aPoint.setLongitude(temp);
                        } else if(insideLocations){
                            // lat/long outside a known point tag (e.g. <pointByCoordinates>):
                            // keep it as the location's display point
                            if(aRecord.getLocation().getDisplayPoint() == null){
                                aRecord.getLocation().setDisplayPoint(new GeoPoint());
                            }
                            aRecord.getLocation().getDisplayPoint().setLongitude(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("tpegDirection")) {
                        String temp = xpp.nextText();
                        if(insideLocations){
                            aRecord.getLocation().setDirection(temp);
                        }
                    } else if (tagName.endsWith("DescriptorType")) {
                        // comes after the <value> it describes, e.g. "M8" then "linkName"
                        String temp = xpp.nextText();
                        if(insideAPoint && descriptorValue != null){
                            aRecord.getLocation().addDescriptor(temp, descriptorValue);
                            if(temp.equalsIgnoreCase("linkName") || temp.equalsIgnoreCase("tpegIlcName1")){
                                // the road, e.g. "M8"
                                if(aRecord.getLocation().getRoadName() == null){
                                    aRecord.getLocation().setRoadName(descriptorValue);
                                }
                            } else if(temp.equalsIgnoreCase("intersectionName") || temp.equalsIgnoreCase("nonLinkedPointName")){
                                // the full place name, e.g. "M8 J19 Kingston Bridge (East)" - always use it
                                aPoint.setName(descriptorValue);
                            } else if(aPoint.getName() == null){
                                aPoint.setName(descriptorValue);   // e.g. "J19", if nothing better is given
                            }
                            descriptorValue = null;
                        }
                    } else if (tagName.equalsIgnoreCase("roadNumber") || tagName.equalsIgnoreCase("roadName")) {
                        String temp = xpp.nextText();
                        if(insideARecord && aRecord.getLocation().getRoadName() == null){
                            aRecord.getLocation().setRoadName(temp); // keeps just the road number
                        }
                    }

                    // ---------------- type-specific details ----------------
                    else if (tagName.equals("accidentType")
                            || tagName.equals("vehicleObstructionType")
                            || tagName.equals("obstructionType")
                            || tagName.equals("environmentalObstructionType")
                            || tagName.equals("animalPresenceType")
                            || tagName.equals("abnormalTrafficType")
                            || tagName.equals("poorEnvironmentType")
                            || tagName.equals("weatherRelatedRoadConditionType")
                            || tagName.equals("nonWeatherRelatedRoadConditionType")
                            || tagName.equals("equipmentOrSystemFaultType")
                            || tagName.equals("infrastructureDamageType")
                            || tagName.equals("publicEventType")
                            || tagName.equals("roadMaintenanceType")
                            || tagName.equals("constructionWorkType")
                            || tagName.equals("roadOrCarriagewayOrLaneManagementType")
                            || tagName.equals("generalNetworkManagementType")) {
                        // e.g. <accidentType>collision</accidentType>
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.setDetailType(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("roadworksDuration")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.setRoadworksDuration(temp);
                        }
                    } else if (tagName.equalsIgnoreCase("roadworksScale")) {
                        String temp = xpp.nextText();
                        if(insideARecord){
                            aRecord.setRoadworksScale(temp);
                        }
                    }
                    // Any other tag is ignored, so new fields in the feed won't crash the app
                } else if(eventType == XmlPullParser.END_TAG) { // Found an end tag
                    String tagName = xpp.getName();

                    if (tagName.equalsIgnoreCase("situation")) {
                        if(insideASituation){
                            allSituations.add(aSituation); //add to collection
                            Log.d("MyTag","Situation parsing completed!");
                        }
                        insideASituation = false;
                    } else if (tagName.equalsIgnoreCase("situationRecord")) {
                        if(insideASituation && insideARecord){
                            // no road number in the location? look for one in the comment instead
                            if(aRecord.getLocation().getRoadName() == null){
                                aRecord.getLocation().setRoadName(aRecord.getComment());
                            }
                            aSituation.addRecord(aRecord); //add record to its situation
                            Log.d("MyTag","Situation record completed! Road is "
                                    + aRecord.getLocation().getRoadName());
                        }
                        insideARecord = false;
                        insideComment = false;
                        insideLocations = false;
                        insideAPoint = false;
                    } else if (tagName.equalsIgnoreCase("generalPublicComment")) {
                        insideComment = false;
                    } else if (tagName.equalsIgnoreCase("groupOfLocations")) {
                        insideLocations = false;
                    } else if (insideAPoint && tagName.equals(pointTagName)) {
                        //add the finished point to the location
                        SituationLocation location = aRecord.getLocation();
                        if((pointTagName.equals("from") || pointTagName.equals("linearCoordinatesStartPoint"))
                                && location.getFrom() == null){
                            location.setFrom(aPoint);
                        } else if((pointTagName.equals("to") || pointTagName.equals("linearCoordinatesEndPoint"))
                                && location.getTo() == null){
                            location.setTo(aPoint);
                        } else if(pointTagName.equals("point")){
                            location.setPoint(aPoint);
                        } else if(pointTagName.equals("locationForDisplay")){
                            location.setDisplayPoint(aPoint);
                        }
                        Log.d("MyTag", pointTagName + " point completed: " + aPoint);
                        insideAPoint = false;
                        descriptorValue = null;
                    }
                }
                eventType = xpp.next(); // Get the next event before looping again
            } // End of while
        // a parse error keeps whatever situations were read before the error
        } catch (XmlPullParserException ae1) {
            Log.e("MyTag","Parsing error" + ae1.toString());
        } catch (IOException ae1) {
            Log.e("MyTag","IO error during parsing");
        }

        Log.d("MyTag","End of document reached, " + allSituations.size() + " " + feedType + " situations");
        return allSituations;
    } // End of parseData

}