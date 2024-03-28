package org.example;

import java.util.HashMap;
import java.util.Map;

public class LRUCache {
    Node head = new Node("","");
    Node tail = new Node("","");

    HashMap<String,Node> map;
    int limit;

    int capacity=0;

    public LRUCache(int capacity) {
        map = new HashMap<>();
        this.limit = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public String get(String key) {
        if(map.containsKey(key)){
            Node node = map.get(key);
            removeNode(node);
            insert(node);
            return node.value;
        }
        return "-1";
    }

    public void put(String key, String value) {
        if(map.containsKey(key)){
            Node node = map.get(key);
            node.key = key;
            node.value = value;

            removeNode(node);
            insert(node);
            return;
        }
        if(capacity == limit){
            delete();
        }
        // add new node
        Node node = new Node(key,value);
        insert(node);
    }

    public void removeNode(Node node){
        node.next.prev = node.prev;
        node.prev.next = node.next;
        capacity--;
    }

    void delete(){
        Node delNode = tail.prev;
        tail.prev = tail.prev.prev;
        tail.prev.next = tail;

        //deleting from hashmap
        map.remove(delNode.key);
        capacity--;

    }

    void insert(Node node){
        node.next = head.next;
        node.next.prev = node;

        node.prev = head;
        head.next = node;

        map.put(node.key,node);
        capacity++;
    }

    void printCache(){
        System.out.println(map);
    }


    class Node{
        String key,value;
        Node prev,next;

        Node(String key,String value){
            this.key = key;
            this.value = value;
        }

        Node(Node prev,Node next){
            this.prev = null;
            this.next = null;
        }
    }
}
