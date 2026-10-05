package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

/**
 * Lifecare bevakning answers, shaped after captures of Lifecare's own web app (2026-09-23) and trimmed. Test persons
 * only.
 */
public final class LifecareReminderFixtures {

	private LifecareReminderFixtures() {}

	/** Reminders/GetProposalForService (capture 2026-09-23), trimmed. */
	public static final String PROPOSAL = """
		{
		  "reminderTypeObjects": [
		    { "id": 7012, "text": "IFO.Beslut", "associations": [
		      { "key": 27, "value": "2026-09-02 : Ek Ekonomiskt bistånd", "caseworkerId": "TEST",
		        "reminderTypes": [ { "id": 2, "text": "Manuell bevakning beslut", "activeStatus": 1, "objectId": 7012, "isActive": true } ] } ] },
		    { "id": 7083, "text": "IFO.Insats", "associations": [
		      { "key": 2, "value": "2026-08-26 : EK Ekonomiskt bistånd", "caseworkerId": "TEST",
		        "reminderTypes": [ { "id": 3, "text": "Manuell bevakning insats", "activeStatus": 1, "objectId": 7083, "isActive": true } ] } ] }
		  ],
		  "options": {
		    "reminderReceiverTypes": [ { "code": 1, "text": "Handläggare", "isActive": true } ],
		    "reminderCaseworkers": [
		      { "id": "RPA_031DEV", "name": "RPA_031DEV", "isCaseworker": true },
		      { "id": "TEST", "name": "Test Handläggare", "isCaseworker": true } ],
		    "reminderPriorityTypes": [
		      { "code": 1, "text": "Hög", "isActive": true },
		      { "code": 2, "text": "Normal", "isActive": true },
		      { "code": 5, "text": "Utgången", "isActive": false } ],
		    "reminderStatusTypes": [
		      { "code": 2, "text": "Klar", "isActive": true },
		      { "code": 3, "text": "Ej påbörjad", "isActive": true } ]
		  },
		  "reminderForAdd": {
		    "reminderId": 0, "receiverType": 1, "objectType": 0, "objectTypeName": null, "objectId": null, "objectPropertyId": 0,
		    "mainObjectType": 7083, "mainObjectId": "2", "personId": null, "personName": null, "caseworkerId": null, "caseworkerName": null,
		    "reminderDate": "", "status": 3, "statusText": null, "priority": 2, "priorityText": null, "type": 0, "typeText": null,
		    "text": null, "startComponent1": 0, "startComponent2": 0, "updateTimestamp": "", "updateSignature": null,
		    "objectId2": null, "info": null, "customerId": 0, "personIdFormatted": ""
		  }
		}
		""";

	/** Bevakning 40 as the editor holds it before the change. */
	public static final String CURRENT = """
		{
		  "reminderId": 40, "receiverType": 1, "objectType": 7083, "objectTypeName": "IFO.Insats", "objectId": "2", "objectPropertyId": 0,
		  "mainObjectType": 7083, "mainObjectId": "2", "personId": "199001122390", "personName": "Jeppson, Test", "caseworkerId": "TEST",
		  "caseworkerName": "Test Handläggare", "reminderDate": "2026-09-23", "status": 3, "statusText": "Ej påbörjad", "priority": 2,
		  "priorityText": "Normal", "type": 3, "typeText": "Manuell bevakning insats", "text": "Hej", "startComponent1": 0,
		  "startComponent2": 0, "updateTimestamp": "2026-09-23", "updateSignature": "lis09bre", "objectId2": "", "info": "",
		  "customerId": 3, "personIdFormatted": "900112-2390"
		}
		""";
}
