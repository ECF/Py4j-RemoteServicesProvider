'''
Created on Nov 21, 2017

@author: slewis
'''

from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec

from osgiservicebridge.flatbuf import flatbuf_remote_service, flatbuf_remote_service_method
import logging
import sys
timing = logging.getLogger("timing.osgiservicebridge.protobuf")

timing.setLevel(logging.DEBUG)
ch = logging.StreamHandler(stream = sys.stdout)
ch.setLevel(logging.DEBUG)

timing.addHandler(ch)

import org.eclipse.ecf.examples.flatbuf.monster.Monster as Monster
import org.eclipse.ecf.examples.flatbuf.monster.Weapon as Weapon
import org.eclipse.ecf.examples.flatbuf.monster.Color as Color
import org.eclipse.ecf.examples.flatbuf.monster.Equipment as Equipment
import org.eclipse.ecf.examples.flatbuf.monster.Vec3 as Vec3

import flatbuffers

@flatbuf_remote_service(objectClass=['org.eclipse.ecf.examples.flatbuf.monster.IScare'])
class MonsterServiceImpl:
    
    def createMonster(self):
        builder = flatbuffers.Builder(0)

        # Create some weapons for our Monster ('Sword' and 'Axe').
        weapon_one = builder.CreateString('Sword')
        weapon_two = builder.CreateString('Axe')
    
        Weapon.WeaponStart(builder)
        Weapon.WeaponAddName(builder, weapon_one)
        Weapon.WeaponAddDamage(builder, 3)
        sword = Weapon.WeaponEnd(builder)
    
        Weapon.WeaponStart(builder)
        Weapon.WeaponAddName(builder, weapon_two)
        Weapon.WeaponAddDamage(builder, 5)
        axe = Weapon.WeaponEnd(builder)
    
        # Serialize the FlatBuffer data.
        name = builder.CreateString('PythonOrc')
    
        Monster.MonsterStartInventoryVector(builder, 10)
        # Note: Since we prepend the bytes, this loop iterates in reverse order.
        for i in reversed(range(0, 10)):
            builder.PrependByte(i)
        inv = builder.EndVector(10)
    
        Monster.MonsterStartWeaponsVector(builder, 2)
        # Note: Since we prepend the data, prepend the weapons in reverse order.
        builder.PrependUOffsetTRelative(axe)
        builder.PrependUOffsetTRelative(sword)
        weapons = builder.EndVector(2)
    
        pos = Vec3.CreateVec3(builder, 1.0, 2.0, 3.0)
    
        Monster.MonsterStart(builder)
        Monster.MonsterAddPos(builder, pos)
        Monster.MonsterAddHp(builder, 300)
        Monster.MonsterAddName(builder, name)
        Monster.MonsterAddInventory(builder, inv)
        Monster.MonsterAddColor(builder,
                                            Color.Color().Red)
        Monster.MonsterAddWeapons(builder, weapons)
        Monster.MonsterAddEquippedType(
          builder, Equipment.Equipment().Weapon)
        Monster.MonsterAddEquipped(builder, axe)
        orc = Monster.MonsterEnd(builder)
    
        builder.Finish(orc)
        
        return builder
        
    @flatbuf_remote_service_method(arg_type=Monster.Monster,return_type=Monster.Monster)
    def scareWith(self,monster):
        print("scareWidth called with monster name="+monster.Name())
        return self.createMonster()
    
if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    hsid = bridge.export(MonsterServiceImpl())
    print("exported")
    _wait_for_sec(20)
    bridge.unexport(hsid)
    print("unexported")
    bridge.disconnect()
    print("disconnected...exiting")
